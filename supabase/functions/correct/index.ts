/**
 * POST /correct
 *
 * "Not Spam" correction signal. Increments negative_signals for a number
 * and recomputes the confidence score. If negative_signals >= 5, the score
 * is dampened proportionally.
 *
 * Body (JSON):
 *   {
 *     number_hash:       string,
 *     device_token_hash: string,
 *   }
 *
 * Response:
 *   { success: true, confidence_score: number }
 */

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { handleCors } from "../_shared/cors.ts";
import { errorResponse, jsonResponse } from "../_shared/errors.ts";
import { checkRateLimit } from "../_shared/rateLimit.ts";
import { computeConfidenceScore } from "../_shared/confidence.ts";

Deno.serve(async (req: Request) => {
  const corsResult = handleCors(req);
  if (corsResult) return corsResult;

  if (req.method !== "POST") {
    return errorResponse("Method not allowed", 405);
  }

  let body: { number_hash?: string; device_token_hash?: string };
  try {
    body = await req.json();
  } catch {
    return errorResponse("Invalid JSON body", 400);
  }

  const { number_hash, device_token_hash } = body;

  if (!number_hash || !device_token_hash) {
    return errorResponse("Missing required fields: number_hash, device_token_hash", 400);
  }

  if (!/^[a-f0-9]{64}$/.test(number_hash) || !/^[a-f0-9]{64}$/.test(device_token_hash)) {
    return errorResponse("Invalid hash format", 400);
  }

  // Rate limit: 10 corrections per device per hour (prevent score manipulation)
  const rateLimitKey = `correct:${device_token_hash}`;
  if (!checkRateLimit(rateLimitKey, 10, 3600)) {
    return errorResponse("Rate limit exceeded", 429);
  }

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
  );

  const { data: current } = await supabase
    .from("reputation")
    .select("unique_reporters, negative_signals, last_reported_at")
    .eq("number_hash", number_hash)
    .single();

  if (!current) {
    // Number not in reputation table — correction is a no-op
    return jsonResponse({ success: true, confidence_score: 0 });
  }

  const newNegativeSignals = (current.negative_signals ?? 0) + 1;
  const lastReportedAt = current.last_reported_at ? new Date(current.last_reported_at) : null;

  const newConfidenceScore = computeConfidenceScore(
    current.unique_reporters,
    newNegativeSignals,
    lastReportedAt,
  );

  await supabase
    .from("reputation")
    .update({
      negative_signals: newNegativeSignals,
      confidence_score: newConfidenceScore,
      last_computed_at: new Date().toISOString(),
    })
    .eq("number_hash", number_hash);

  return jsonResponse({ success: true, confidence_score: newConfidenceScore });
});
