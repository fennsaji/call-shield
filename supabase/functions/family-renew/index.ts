/**
 * POST /family-renew
 *
 * Reactivates all dependent pairings for a guardian device and refreshes the subscription expiry.
 * Called when the guardian's subscription auto-renews or they upgrade to a family plan.
 *
 * Body (JSON):
 *   {
 *     guardian_device_hash:    string,    // HMAC-SHA256 of the guardian's device UUID (hex64)
 *     plan_type:               string,    // e.g. "FAMILY_ANNUAL" | "FAMILY_LIFETIME"
 *     subscription_expires_at?: string   // ISO timestamp or absent (lifetime plans)
 *   }
 *
 * Response:
 *   200 { success: true, renewed_count: N }
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

  let body: {
    guardian_device_hash?: string;
    plan_type?: string;
    subscription_expires_at?: string;
  };
  try {
    body = await req.json();
  } catch {
    return errorResponse("Invalid JSON body", 400);
  }

  const { guardian_device_hash, plan_type, subscription_expires_at } = body;
  if (!guardian_device_hash || !plan_type) {
    return errorResponse("Missing required fields: guardian_device_hash, plan_type", 400);
  }
  if (!HEX64_RE.test(guardian_device_hash)) {
    return errorResponse("Invalid guardian_device_hash format", 400);
  }

  // Rate limit: 10 renewals per guardian hash per hour
  if (!checkRateLimit(`renew:${guardian_device_hash}`, 10, 3600)) {
    return errorResponse("Rate limit exceeded", 429);
  }

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
  );

  const { data, error } = await supabase
    .from("family_pairing")
    .update({
      subscription_active: true,
      plan_type,
      subscription_expires_at: subscription_expires_at ?? null,
    })
    .eq("guardian_device_hash", guardian_device_hash)
    .select("token_hash");

  if (error) return errorResponse("Failed to renew pairings", 500);

  return jsonResponse({ success: true, renewed_count: data?.length ?? 0 });
});
