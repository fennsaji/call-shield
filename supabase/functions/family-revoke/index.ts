/**
 * POST /family-revoke
 *
 * Immediately deactivates all dependent pairings belonging to a guardian device.
 * Called when the guardian's subscription is cancelled or downgraded to a non-family plan.
 * After revocation, dependents' next /family-sync call returns 402.
 *
 * Body (JSON):
 *   { guardian_device_hash: string }  // HMAC-SHA256 of the guardian's device UUID (hex64)
 *
 * Response:
 *   200 { success: true, revoked_count: N }
 *   400 if malformed
 *   429 rate limited
 */

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { handleCors } from "../_shared/cors.ts";
import { errorResponse, jsonResponse } from "../_shared/errors.ts";
import { checkRateLimit } from "../_shared/rateLimit.ts";

const HEX64_RE = /^[a-f0-9]{64}$/;

Deno.serve(async (req: Request) => {
  const corsResult = handleCors(req);
  if (corsResult) return corsResult;

  if (req.method !== "POST") return errorResponse("Method not allowed", 405);

  let body: { guardian_device_hash?: string };
  try {
    body = await req.json();
  } catch {
    return errorResponse("Invalid JSON body", 400);
  }

  const { guardian_device_hash } = body;
  if (!guardian_device_hash) {
    return errorResponse("Missing required field: guardian_device_hash", 400);
  }
  if (!HEX64_RE.test(guardian_device_hash)) {
    return errorResponse("Invalid guardian_device_hash format", 400);
  }

  // Rate limit: 10 revocations per guardian hash per hour
  if (!checkRateLimit(`revoke:${guardian_device_hash}`, 10, 3600)) {
    return errorResponse("Rate limit exceeded", 429);
  }

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
  );

  const { data, error } = await supabase
    .from("family_pairing")
    .update({ subscription_active: false })
    .eq("guardian_device_hash", guardian_device_hash)
    .eq("subscription_active", true)
    .select("token_hash");

  if (error) return errorResponse("Failed to revoke pairings", 500);

  return jsonResponse({ success: true, revoked_count: data?.length ?? 0 });
});
