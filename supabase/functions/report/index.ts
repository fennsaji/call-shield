/**
 * POST /report
 *
 * Submits a spam report for a hashed phone number.
 * Rate-limited to 20 reports per device per hour.
 * Enforces reporter deduplication — a device contributes to unique_reporters only once per number.
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

  // Fetch current reputation state
  const { data: current } = await supabase
    .from("reputation")
    .select("report_count, unique_reporters, negative_signals, last_reported_at, category")
    .eq("number_hash", number_hash)
    .single();

  const newReportCount = (current?.report_count ?? 0) + 1;
  const newUniqueReporters = (current?.unique_reporters ?? 0) + (isNewReporter ? 1 : 0);
  const negativeSignals = current?.negative_signals ?? 0;
  const now = new Date();

  // Determine dominant category (simple: use the newly reported one if unique reporters > existing)
  // Full category voting is Phase 2 (number_categories table). Phase 1 uses last-write wins for category.
  const newCategory = category;

  const newConfidenceScore = computeConfidenceScore(newUniqueReporters, negativeSignals, now);

  // Upsert reputation record
  await supabase.from("reputation").upsert({
    number_hash,
    report_count: newReportCount,
    unique_reporters: newUniqueReporters,
    confidence_score: newConfidenceScore,
    category: newCategory,
    last_reported_at: now.toISOString(),
    last_computed_at: now.toISOString(),
  });

  return jsonResponse({
    success: true,
    confidence_score: newConfidenceScore,
    unique_reporters: newUniqueReporters,
  });
});
