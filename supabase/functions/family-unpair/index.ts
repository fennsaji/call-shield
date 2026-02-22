/**
 * DELETE /family-unpair
 *
 * Removes all family sync data for a pairing token.
 * Called by either the guardian or dependent when they choose to unpair.
 * After this call, no further rule sync is possible for this token.
 *
 * Body (JSON):
 *   { token_hash: string }  // HMAC-SHA256 of the pairing UUID
 *
 * Response:
 *   200 { success: true }
 */

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { handleCors } from "../_shared/cors.ts";
import { errorResponse, jsonResponse } from "../_shared/errors.ts";

Deno.serve(async (req: Request) => {
  const corsResult = handleCors(req);
  if (corsResult) return corsResult;

  if (req.method !== "DELETE") return errorResponse("Method not allowed", 405);

  let body: { token_hash?: string };
  try {
    body = await req.json();
  } catch {
    return errorResponse("Invalid JSON body", 400);
  }

  const { token_hash } = body;
  if (!token_hash) return errorResponse("Missing required field: token_hash", 400);
  if (!/^[a-f0-9]{64}$/.test(token_hash)) {
    return errorResponse("Invalid token_hash format", 400);
  }

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
  );

  // Delete sync rules and pairing record — order matters (FK if added later)
  await supabase.from("family_sync_rules").delete().eq("token_hash", token_hash);
  await supabase.from("family_pairing").delete().eq("token_hash", token_hash);

  // No error check — idempotent: deleting a non-existent token is a success
  return jsonResponse({ success: true });
});
