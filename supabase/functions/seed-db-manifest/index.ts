/**
 * GET /seed-db-manifest
 *
 * Returns the current seed spam database version and SHA-256 checksum.
 * The Android client uses this to decide whether to download a new delta.
 *
 * Response:
 *   { version: number, sha256: string, storage_url: string }
 */

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { handleCors } from "../_shared/cors.ts";
import { errorResponse, jsonResponse } from "../_shared/errors.ts";

Deno.serve(async (req: Request) => {
  const corsResult = handleCors(req);
  if (corsResult) return corsResult;

  if (req.method !== "GET") {
    return errorResponse("Method not allowed", 405);
  }

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
  );

  const { data, error } = await supabase
    .from("seed_db_versions")
    .select("version, sha256_checksum, storage_path")
    .eq("is_current", true)
    .single();

  if (error || !data) {
    return errorResponse("No seed DB version available", 404);
  }

  // Generate a short-lived signed URL for the storage file (1 hour)
  const { data: signedUrl, error: urlError } = await supabase
    .storage
    .from("seed-db")
    .createSignedUrl(data.storage_path, 3600);

  if (urlError || !signedUrl) {
    return errorResponse("Failed to generate download URL", 500);
  }

  return jsonResponse({
    version: data.version,
    sha256: data.sha256_checksum,
    download_url: signedUrl.signedUrl,
  });
});
