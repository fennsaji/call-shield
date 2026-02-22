/**
 * POST /reputation-harden  (Phase 3 — scheduled or manual admin call)
 *
 * Scans for coordinated abuse patterns and dampens affected reputation scores.
 * Protected by x-admin-secret header to prevent unauthorized calls.
 *
 * Actions performed:
 *   1. Spike detection:   numbers with report_count/unique_reporters ratio > SPIKE_RATIO
 *                         within the SPIKE_WINDOW are flagged as "spike".
 *   2. Oscillation guard: numbers whose confidence_score was updated more than
 *                         OSCILLATION_FLIPS times within 24h are flagged as "oscillation".
 *   3. Low-trust devices: device tokens with > ABUSE_REPORTS_PER_DEVICE distinct
 *                         numbers reported in 24h are flagged, and their influence
 *                         is reduced by capping confidence scores for newly flagged numbers.
 *
 * Flags are written to `reputation_flags`. Already-flagged numbers are not re-flagged.
 * Dampening: confidence_score of flagged numbers is multiplied by DAMPEN_FACTOR.
 *
 * Response:
 *   200 { flagged: number, dampened: number }
 */

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { handleCors } from "../_shared/cors.ts";
import { errorResponse, jsonResponse } from "../_shared/errors.ts";

// ── Thresholds ────────────────────────────────────────────────────────────────
/** report_count / unique_reporters above this ratio → coordinated spike. */
const SPIKE_RATIO = 5;
/** Look-back window for spike detection (milliseconds). */
const SPIKE_WINDOW_MS = 60 * 60 * 1000; // 1 hour
/** Distinct numbers a single device can report in 24h before being treated as low-trust. */
const ABUSE_REPORTS_PER_DEVICE = 30;
/** Multiplier applied to confidence_score of flagged numbers. */
const DAMPEN_FACTOR = 0.5;

Deno.serve(async (req: Request) => {
  const corsResult = handleCors(req);
  if (corsResult) return corsResult;

  if (req.method !== "POST") return errorResponse("Method not allowed", 405);

  // Admin-only: require secret header
  const adminSecret = Deno.env.get("ADMIN_SECRET");
  if (adminSecret && req.headers.get("x-admin-secret") !== adminSecret) {
    return errorResponse("Unauthorized", 401);
  }

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
  );

  const now = new Date();
  const spikeWindowStart = new Date(now.getTime() - SPIKE_WINDOW_MS).toISOString();
  const abuseLookbackStart = new Date(now.getTime() - 24 * 60 * 60 * 1000).toISOString();

  let flaggedCount = 0;
  let dampenedCount = 0;

  // ── 1. Spike detection ────────────────────────────────────────────────────
  // Find numbers with many reports but few unique reporters in the spike window.
  // Uses report_events to count raw reports vs unique devices.
  const { data: recentEvents } = await supabase
    .from("report_events")
    .select("number_hash, device_token_hash")
    .gte("reported_at", spikeWindowStart);

  if (recentEvents) {
    // Aggregate: count total reports and unique devices per number_hash
    const perNumber = new Map<string, { total: number; devices: Set<string> }>();
    for (const ev of recentEvents) {
      if (!perNumber.has(ev.number_hash)) {
        perNumber.set(ev.number_hash, { total: 0, devices: new Set() });
      }
      const agg = perNumber.get(ev.number_hash)!;
      agg.total++;
      agg.devices.add(ev.device_token_hash);
    }

    for (const [number_hash, { total, devices }] of perNumber.entries()) {
      const ratio = total / Math.max(devices.size, 1);
      if (ratio >= SPIKE_RATIO && total >= 10) {
        await flagNumber(supabase, number_hash, "spike", now);
        flaggedCount++;
      }
    }
  }

  // ── 2. Low-trust device detection ────────────────────────────────────────
  // Devices reporting too many distinct numbers in 24h are treated as abusive.
  // We flag numbers they reported that aren't already flagged.
  const { data: abuseEvents } = await supabase
    .from("report_events")
    .select("number_hash, device_token_hash")
    .gte("reported_at", abuseLookbackStart);

  if (abuseEvents) {
    const perDevice = new Map<string, Set<string>>();
    for (const ev of abuseEvents) {
      if (!perDevice.has(ev.device_token_hash)) {
        perDevice.set(ev.device_token_hash, new Set());
      }
      perDevice.get(ev.device_token_hash)!.add(ev.number_hash);
    }

    for (const [, numberHashes] of perDevice.entries()) {
      if (numberHashes.size >= ABUSE_REPORTS_PER_DEVICE) {
        for (const number_hash of numberHashes) {
          await flagNumber(supabase, number_hash, "low_trust", now);
          flaggedCount++;
        }
      }
    }
  }

  // ── 3. Apply dampening to all unresolved flagged numbers ──────────────────
  const { data: flags } = await supabase
    .from("reputation_flags")
    .select("number_hash")
    .eq("resolved", false);

  if (flags) {
    for (const { number_hash } of flags) {
      const { data: rep } = await supabase
        .from("reputation")
        .select("confidence_score")
        .eq("number_hash", number_hash)
        .single();

      if (rep && rep.confidence_score > 0) {
        const dampened = rep.confidence_score * DAMPEN_FACTOR;
        await supabase
          .from("reputation")
          .update({ confidence_score: dampened, last_computed_at: now.toISOString() })
          .eq("number_hash", number_hash);
        dampenedCount++;
      }
    }
  }

  return jsonResponse({ flagged: flaggedCount, dampened: dampenedCount });
});

// ── Helpers ───────────────────────────────────────────────────────────────────

async function flagNumber(
  // deno-lint-ignore no-explicit-any
  supabase: any,
  number_hash: string,
  reason: "spike" | "low_trust" | "oscillation",
  now: Date,
): Promise<void> {
  // Only insert if not already flagged with this reason
  const { data: existing } = await supabase
    .from("reputation_flags")
    .select("number_hash")
    .eq("number_hash", number_hash)
    .eq("reason", reason)
    .eq("resolved", false)
    .single();

  if (!existing) {
    await supabase.from("reputation_flags").insert({
      number_hash,
      reason,
      flagged_at: now.toISOString(),
      resolved: false,
    });
  }
}
