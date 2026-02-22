/**
 * POST /family-pair
 *
 * Registers a new pairing token from the guardian device.
 * The guardian generates a UUID locally, computes HMAC-SHA256(token, salt), and sends the hash.
 * The raw token never reaches the backend.
 *
 * Body (JSON):
 *   {
 *     token_hash:  string,   // HMAC-SHA256 of the pairing UUID
 *     expires_at:  string,   // ISO timestamp, max 10 minutes from now
 *   }
 *
 * Response:
 *   201 { success: true }
 *   400 if malformed / hash already active
 *   429 rate limited
 */

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { handleCors } from "../_shared/cors.ts";
import { errorResponse, jsonResponse } from "../_shared/errors.ts";
import { checkRateLimit } from "../_shared/rateLimit.ts";

const MAX_EXPIRY_MS = 10 * 60 * 1000; // 10 minutes

Deno.serve(async (req: Request) => {
  const corsResult = handleCors(req);
  if (corsResult) return corsResult;

  if (req.method !== "POST") return errorResponse("Method not allowed", 405);

  let body: { token_hash?: string; expires_at?: string };
  try {
    body = await req.json();
  } catch {
    return errorResponse("Invalid JSON body", 400);
  }

  const { token_hash, expires_at } = body;
  if (!token_hash || !expires_at) {
    return errorResponse("Missing required fields: token_hash, expires_at", 400);
  }
  if (!/^[a-f0-9]{64}$/.test(token_hash)) {
    return errorResponse("Invalid token_hash format", 400);
  }

  const expiryDate = new Date(expires_at);
  const now = Date.now();
  if (isNaN(expiryDate.getTime()) || expiryDate.getTime() - now > MAX_EXPIRY_MS) {
    return errorResponse("expires_at must be within 10 minutes from now", 400);
  }
  if (expiryDate.getTime() < now) {
    return errorResponse("expires_at is in the past", 400);
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
  }, { onConflict: "token_hash" });

  if (error) return errorResponse("Failed to register pairing token", 500);

  return jsonResponse({ success: true }, 201);
});
