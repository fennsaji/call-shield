/**
 * GET /reputation
 *
 * Looks up the reputation of a hashed phone number.
 * Rate-limited to 60 requests per device per hour.
 *
 * Query params:
 *   hash          - HMAC-SHA256 hash of the phone number
 *   device_token  - HMAC-SHA256 hash of the device token
 *
 * Response:
 *   { confidence_score, category, report_count, unique_reporters } | { confidence_score: 0 }
 */

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { handleCors } from "../_shared/cors.ts";
import { errorResponse, jsonResponse } from "../_shared/errors.ts";
import { checkRateLimit } from "../_shared/rateLimit.ts";
import { LIKELY_SPAM_THRESHOLD } from "../_shared/confidence.ts";

Deno.serve(async (req: Request) => {
  const corsResult = handleCors(req);
  if (corsResult) return corsResult;

  if (req.method !== "GET") {
    return errorResponse("Method not allowed", 405);
  }

  const url = new URL(req.url);
  const numberHash = url.searchParams.get("hash");
  const deviceTokenHash = url.searchParams.get("device_token");

  if (!numberHash || !deviceTokenHash) {
    return errorResponse("Missing required parameters: hash, device_token", 400);
  }

  // Validate hash format (64-char hex = SHA-256 output)
  if (!/^[a-f0-9]{64}$/.test(numberHash) || !/^[a-f0-9]{64}$/.test(deviceTokenHash)) {
    return errorResponse("Invalid hash format", 400);
  }

  // Rate limit: 60 lookups per device per hour
  const rateLimitKey = `reputation:${deviceTokenHash}`;
  if (!checkRateLimit(rateLimitKey, 60, 3600)) {
    return errorResponse("Rate limit exceeded", 429);
  }

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
  );

  const { data, error } = await supabase
    .from("reputation")
    .select("confidence_score, category, report_count, unique_reporters, negative_signals")
    .eq("number_hash", numberHash)
    .single();

  if (error || !data) {
    // Not found — return zero score, not an error
    return jsonResponse({ confidence_score: 0, category: null, report_count: 0, unique_reporters: 0 });
  }

  return jsonResponse({
    confidence_score: data.confidence_score,
    category: data.category,
    report_count: data.report_count,
    unique_reporters: data.unique_reporters,
  });
});
