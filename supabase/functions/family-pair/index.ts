/**
 * POST /family-pair
 *
 * Registers a new pairing token from the guardian device.
 * The guardian generates a UUID locally, computes HMAC-SHA256(token, salt), and sends the hash.
 * The raw token never reaches the backend.
 *
 * Body (JSON):
 *   {
 *     token_hash:            string,   // HMAC-SHA256 of the pairing UUID
 *     expires_at:            string,   // ISO timestamp, max 10 minutes from now
 *     guardian_device_hash:  string,   // HMAC-SHA256 of the guardian's device UUID (hex64)
 *     plan_type:             string,   // e.g. "FAMILY_ANNUAL" | "FAMILY_LIFETIME"
 *     subscription_expires_at?: string // ISO timestamp or absent (lifetime plans)
 *   }
 *
 * Response:
 *   201 { success: true }
 *   400 if malformed / missing required fields
 *   409 if token already paired
 *   429 rate limited
 */

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { handleCors } from "../_shared/cors.ts";
import { errorResponse, jsonResponse } from "../_shared/errors.ts";
import { checkRateLimit } from "../_shared/rateLimit.ts";

const MAX_EXPIRY_MS = 10 * 60 * 1000; // 10 minutes
const HEX64_RE = /^[a-f0-9]{64}$/;

Deno.serve(async (req: Request) => {
  const corsResult = handleCors(req);
  if (corsResult) return corsResult;

  if (req.method !== "POST") return errorResponse("Method not allowed", 405);

  let body: {
    token_hash?: string;
    expires_at?: string;
    guardian_device_hash?: string;
    plan_type?: string;
    subscription_expires_at?: string;
  };
  try {
    body = await req.json();
  } catch {
    return errorResponse("Invalid JSON body", 400);
  }

  const { token_hash, expires_at, guardian_device_hash, plan_type, subscription_expires_at } = body;

  if (!token_hash || !expires_at || !guardian_device_hash || !plan_type) {
    return errorResponse(
      "Missing required fields: token_hash, expires_at, guardian_device_hash, plan_type",
      400,
    );
  }
  if (!HEX64_RE.test(token_hash)) {
    return errorResponse("Invalid token_hash format", 400);
  }
  if (!HEX64_RE.test(guardian_device_hash)) {
    return errorResponse("Invalid guardian_device_hash format", 400);
  }

  const expiryDate = new Date(expires_at);
  const now = Date.now();
  if (isNaN(expiryDate.getTime()) || expiryDate.getTime() - now > MAX_EXPIRY_MS) {
    return errorResponse("expires_at must be within 10 minutes from now", 400);
  }
  if (expiryDate.getTime() < now) {
    return errorResponse("expires_at is in the past", 400);
  }

  // Validate subscription_expires_at if provided
  if (subscription_expires_at !== undefined && subscription_expires_at !== null) {
    const subExpiry = new Date(subscription_expires_at);
    if (isNaN(subExpiry.getTime()) || subExpiry.getTime() <= now) {
      return errorResponse("subscription_expires_at must be a valid future timestamp", 400);
    }
  }

  // Rate limit: 5 pair requests per device per hour (generous for QR retries)
  if (!checkRateLimit(`pair:${token_hash}`, 5, 3600)) {
    return errorResponse("Rate limit exceeded", 429);
  }

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
  );

  // Check if this token_hash is already actively paired
  const { data: existing } = await supabase
    .from("family_pairing")
    .select("token_hash, paired_at")
    .eq("token_hash", token_hash)
    .single();

  if (existing?.paired_at) {
    return errorResponse("Token already paired", 409);
  }

  // Upsert — allows guardian to regenerate QR (new expiry, same hash)
  const { error } = await supabase.from("family_pairing").upsert({
    token_hash,
    expires_at: expiryDate.toISOString(),
    paired_at: null,
    guardian_device_hash,
    plan_type,
    subscription_expires_at: subscription_expires_at ?? null,
    subscription_active: true,
  }, { onConflict: "token_hash" });

  if (error) return errorResponse("Failed to register pairing token", 500);

  return jsonResponse({ success: true }, 201);
});
