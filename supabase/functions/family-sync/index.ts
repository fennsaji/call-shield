/**
 * POST /family-sync  — Guardian pushes updated rules to Supabase.
 * GET  /family-sync  — Dependent polls for latest rules.
 *
 * Both operations require a valid, paired token_hash.
 *
 * POST body (JSON):
 *   {
 *     token_hash:    string,                // HMAC-SHA256 of the pairing UUID
 *     rule_type:     "prefix"|"preference", // one rule type per request
 *     rule_payload:  object,                // encrypted rule data (JSONB)
 *   }
 *
 * GET query params:
 *   ?token_hash=<hex64>
 *
 * POST Response:  200 { success: true }
 * GET  Response:  200 { rules: [{ rule_type, rule_payload, updated_at }] }
 */

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { handleCors } from "../_shared/cors.ts";
import { errorResponse, jsonResponse } from "../_shared/errors.ts";
import { checkRateLimit } from "../_shared/rateLimit.ts";

const VALID_RULE_TYPES = ["prefix", "preference"] as const;

Deno.serve(async (req: Request) => {
  const corsResult = handleCors(req);
  if (corsResult) return corsResult;

  if (req.method === "POST") return handlePush(req);
  if (req.method === "GET") return handlePull(req);
  return errorResponse("Method not allowed", 405);
});

// ── Guardian pushes rules ─────────────────────────────────────────────────────

async function handlePush(req: Request): Promise<Response> {
  let body: { token_hash?: string; rule_type?: string; rule_payload?: unknown };
  try {
    body = await req.json();
  } catch {
    return errorResponse("Invalid JSON body", 400);
  }

  const { token_hash, rule_type, rule_payload } = body;
  if (!token_hash || !rule_type || rule_payload === undefined) {
    return errorResponse("Missing required fields: token_hash, rule_type, rule_payload", 400);
  }
  if (!/^[a-f0-9]{64}$/.test(token_hash)) {
    return errorResponse("Invalid token_hash format", 400);
  }
  if (!VALID_RULE_TYPES.includes(rule_type as typeof VALID_RULE_TYPES[number])) {
    return errorResponse(`rule_type must be one of: ${VALID_RULE_TYPES.join(", ")}`, 400);
  }

  // Rate limit: 60 pushes per token per hour
  if (!checkRateLimit(`sync_push:${token_hash}`, 60, 3600)) {
    return errorResponse("Rate limit exceeded", 429);
  }

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
  );

  const pairing = await assertPaired(supabase, token_hash);
  if (pairing instanceof Response) return pairing;

  const { error } = await supabase.from("family_sync_rules").upsert({
    token_hash,
    rule_type,
    rule_payload,
    updated_at: new Date().toISOString(),
  }, { onConflict: "token_hash,rule_type" });

  if (error) return errorResponse("Failed to sync rules", 500);

  return jsonResponse({ success: true });
}

// ── Dependent polls rules ─────────────────────────────────────────────────────

async function handlePull(req: Request): Promise<Response> {
  const url = new URL(req.url);
  const token_hash = url.searchParams.get("token_hash");

  if (!token_hash) return errorResponse("Missing query param: token_hash", 400);
  if (!/^[a-f0-9]{64}$/.test(token_hash)) {
    return errorResponse("Invalid token_hash format", 400);
  }

  // Rate limit: 120 polls per token per hour (roughly every 30s)
  if (!checkRateLimit(`sync_pull:${token_hash}`, 120, 3600)) {
    return errorResponse("Rate limit exceeded", 429);
  }

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
  );

  const pairing = await assertPaired(supabase, token_hash);
  if (pairing instanceof Response) return pairing;

  const { data, error } = await supabase
    .from("family_sync_rules")
    .select("rule_type, rule_payload, updated_at")
    .eq("token_hash", token_hash);

  if (error) return errorResponse("Failed to fetch rules", 500);

  return jsonResponse({ rules: data ?? [] });
}

// ── Helpers ───────────────────────────────────────────────────────────────────

async function assertPaired(
  // deno-lint-ignore no-explicit-any
  supabase: any,
  token_hash: string,
): Promise<{ token_hash: string } | Response> {
  const { data } = await supabase
    .from("family_pairing")
    .select("token_hash, paired_at, expires_at, subscription_active, subscription_expires_at, guardian_device_hash")
    .eq("token_hash", token_hash)
    .single();

  if (!data) return errorResponse("Token not found", 404);

  // Check 1: immediate revocation flag
  if (data.subscription_active === false) {
    return errorResponse("Guardian subscription is inactive", 402, { reason: "subscription_inactive" });
  }

  // Check 2: passive expiry (only if subscription_expires_at is set — null = lifetime)
  if (data.subscription_expires_at) {
    const expired = new Date(data.subscription_expires_at) < new Date();
    if (expired) {
      // Auto-mark all pairings for this guardian as inactive so future checks are instant
      if (data.guardian_device_hash) {
        await supabase
          .from("family_pairing")
          .update({ subscription_active: false })
          .eq("guardian_device_hash", data.guardian_device_hash);
      }
      return errorResponse("Guardian subscription has expired", 402, { reason: "subscription_expired" });
    }
  }

  if (!data.paired_at) {
    // Mark as paired on first use (dependent calling pull for the first time)
    await supabase
      .from("family_pairing")
      .update({ paired_at: new Date().toISOString() })
      .eq("token_hash", token_hash);
  }
  return { token_hash };
}
