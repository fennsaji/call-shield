/**
 * POST /report
 *
 * Submits a spam report for a hashed phone number.
 * Rate-limited to 20 reports per device per hour.
 * Enforces reporter deduplication — a device contributes to unique_reporters only once per number.
 *
 * Phase 2 additions:
 *   - Category voting via number_categories table (dominant category wins)
 *   - Quarantine queue: numbers with ≥5 reports in 24h are quarantined (velocity guard)
 *
 * Body (JSON):
 *   {
 *     number_hash:       string,   // HMAC-SHA256 of phone number
 *     device_token_hash: string,   // HMAC-SHA256 of device token
 *     category:          string,   // one of the 7 defined categories
 *   }
 *
 * Response:
 *   { success: true, confidence_score: number, unique_reporters: number }
 */

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { handleCors } from "../_shared/cors.ts";
import { errorResponse, jsonResponse } from "../_shared/errors.ts";
import { checkRateLimit } from "../_shared/rateLimit.ts";
import { computeConfidenceScore, MIN_UNIQUE_REPORTERS } from "../_shared/confidence.ts";

const VALID_CATEGORIES = [
  "telemarketing",
  "loan_scam",
  "investment_scam",
  "impersonation",
  "phishing",
  "job_scam",
  "other",
];

/** Number of reports in 60 minutes that triggers quarantine (PRD §9: velocity guard). */
const QUARANTINE_VELOCITY_THRESHOLD = 5;
const QUARANTINE_VELOCITY_WINDOW_MS = 60 * 60 * 1000;  // 60 minutes

Deno.serve(async (req: Request) => {
  const corsResult = handleCors(req);
  if (corsResult) return corsResult;

  if (req.method !== "POST") {
    return errorResponse("Method not allowed", 405);
  }

  let body: { number_hash?: string; device_token_hash?: string; category?: string };
  try {
    body = await req.json();
  } catch {
    return errorResponse("Invalid JSON body", 400);
  }

  const { number_hash, device_token_hash, category } = body;

  if (!number_hash || !device_token_hash || !category) {
    return errorResponse("Missing required fields: number_hash, device_token_hash, category", 400);
  }

  if (!/^[a-f0-9]{64}$/.test(number_hash) || !/^[a-f0-9]{64}$/.test(device_token_hash)) {
    return errorResponse("Invalid hash format", 400);
  }

  if (!VALID_CATEGORIES.includes(category)) {
    return errorResponse(`Invalid category. Must be one of: ${VALID_CATEGORIES.join(", ")}`, 400);
  }

  // Rate limit: 20 reports per device per hour
  const rateLimitKey = `report:${device_token_hash}`;
  if (!checkRateLimit(rateLimitKey, 20, 3600)) {
    return errorResponse("Rate limit exceeded", 429);
  }

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
  );

  // Check deduplication — has this device already reported this number?
  const { data: existing } = await supabase
    .from("reporter_deduplication")
    .select("number_hash")
    .eq("number_hash", number_hash)
    .eq("device_token_hash", device_token_hash)
    .single();

  const isNewReporter = !existing;

  // Always insert the report event (audit trail)
  await supabase.from("report_events").insert({
    number_hash,
    device_token_hash,
    category,
    schema_version: 1,
  });

  // Insert deduplication record if this is a new reporter
  if (isNewReporter) {
    await supabase.from("reporter_deduplication").insert({
      number_hash,
      device_token_hash,
    });
  }

  // ── Phase 2: Category voting ─────────────────────────────────────────────
  // Upsert into number_categories, incrementing vote count for this category.
  await supabase.from("number_categories").upsert(
    {
      number_hash,
      category,
      vote_count: 1,
      last_voted_at: new Date().toISOString(),
    },
    {
      onConflict: "number_hash,category",
      // Use a raw SQL expression for the increment via RPC would be cleaner,
      // but the upsert with ignoreDuplicates = false + merge is the Supabase JS way.
      // We'll fetch + update to ensure accurate counts.
      ignoreDuplicates: false,
    },
  );

  // Fetch current vote for this category and increment
  const { data: existingVote } = await supabase
    .from("number_categories")
    .select("vote_count")
    .eq("number_hash", number_hash)
    .eq("category", category)
    .single();

  if (existingVote && existingVote.vote_count > 1) {
    // Row already existed before our upsert — increment by 1
    await supabase
      .from("number_categories")
      .update({
        vote_count: existingVote.vote_count + 1,
        last_voted_at: new Date().toISOString(),
      })
      .eq("number_hash", number_hash)
      .eq("category", category);
  }

  // Determine dominant category with ≥3 lead over runner-up (PRD §9 category voting rule).
  // Prevents single-actor category flipping — a category must clearly dominate before promotion.
  const { data: allCategoryVotes } = await supabase
    .from("number_categories")
    .select("category, vote_count")
    .eq("number_hash", number_hash)
    .order("vote_count", { ascending: false })
    .limit(2);

  let dominantCategory = category;  // fallback: keep the newly reported category
  if (allCategoryVotes && allCategoryVotes.length > 0) {
    const top = allCategoryVotes[0];
    const runnerUp = allCategoryVotes[1];
    const lead = top.vote_count - (runnerUp?.vote_count ?? 0);
    if (lead >= 3) {
      dominantCategory = top.category;
    } else {
      // Lead < 3: retain the current category on the reputation record (no flip)
      const { data: currentRep } = await supabase
        .from("reputation")
        .select("category")
        .eq("number_hash", number_hash)
        .single();
      dominantCategory = currentRep?.category ?? category;
    }
  }

  // Fetch current reputation state
  const { data: current } = await supabase
    .from("reputation")
    .select("report_count, unique_reporters, negative_signals, last_reported_at")
    .eq("number_hash", number_hash)
    .single();

  const newReportCount = (current?.report_count ?? 0) + 1;
  const newUniqueReporters = (current?.unique_reporters ?? 0) + (isNewReporter ? 1 : 0);
  const negativeSignals = current?.negative_signals ?? 0;
  const now = new Date();

  let newConfidenceScore = computeConfidenceScore(newUniqueReporters, negativeSignals, now);

  // ── Phase 2: Quarantine velocity check ──────────────────────────────────
  // Count reports for this number in the last 60 minutes (PRD §9 velocity window)
  const sinceVelocityWindow = new Date(Date.now() - QUARANTINE_VELOCITY_WINDOW_MS).toISOString();
  const { count: recentReportCount } = await supabase
    .from("report_events")
    .select("id", { count: "exact", head: true })
    .eq("number_hash", number_hash)
    .gte("reported_at", sinceVelocityWindow);

  const isVelocityBurst = (recentReportCount ?? 0) >= QUARANTINE_VELOCITY_THRESHOLD;

  if (isVelocityBurst) {
    // Upsert into quarantine queue
    await supabase.from("quarantine_queue").upsert(
      {
        number_hash,
        trigger_reason: "velocity",
        report_count_24h: recentReportCount ?? QUARANTINE_VELOCITY_THRESHOLD,
        quarantined_at: now.toISOString(),
        expires_at: new Date(Date.now() + 48 * 60 * 60 * 1000).toISOString(),
        reviewed: false,
      },
      { onConflict: "number_hash" },
    );

    // Cap confidence score at 0.75 for quarantined numbers (prevents auto-block until reviewed)
    newConfidenceScore = Math.min(newConfidenceScore, 0.75);
  }

  // Upsert reputation record with dominant category
  await supabase.from("reputation").upsert({
    number_hash,
    report_count: newReportCount,
    unique_reporters: newUniqueReporters,
    confidence_score: newConfidenceScore,
    category: dominantCategory,
    last_reported_at: now.toISOString(),
    last_computed_at: now.toISOString(),
  });

  return jsonResponse({
    success: true,
    confidence_score: newConfidenceScore,
    unique_reporters: newUniqueReporters,
    quarantined: isVelocityBurst,
  });
});
