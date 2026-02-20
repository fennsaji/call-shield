/**
 * POST /verify-subscription
 *
 * Verifies a Google Play subscription purchase token via the Google Play
 * Developer API. Returns the entitlement state (active / expired).
 *
 * Body (JSON):
 *   {
 *     purchase_token:    string,   // from Google Play Billing
 *     product_id:        string,   // e.g. "callguard_pro_annual"
 *     device_token_hash: string,
 *   }
 *
 * Response:
 *   { active: boolean, expiry_time_ms: number | null }
 */

import { handleCors } from "../_shared/cors.ts";
import { errorResponse, jsonResponse } from "../_shared/errors.ts";

const PACKAGE_NAME = "com.fenn.callguard";

const PRODUCT_IDS = ["callguard_pro_annual", "callguard_pro_monthly"];

Deno.serve(async (req: Request) => {
  const corsResult = handleCors(req);
  if (corsResult) return corsResult;

  if (req.method !== "POST") {
    return errorResponse("Method not allowed", 405);
  }

  let body: { purchase_token?: string; product_id?: string; device_token_hash?: string };
  try {
    body = await req.json();
  } catch {
    return errorResponse("Invalid JSON body", 400);
  }

  const { purchase_token, product_id, device_token_hash } = body;

  if (!purchase_token || !product_id || !device_token_hash) {
    return errorResponse("Missing required fields", 400);
  }

  if (!PRODUCT_IDS.includes(product_id)) {
    return errorResponse("Invalid product_id", 400);
  }

  // Get a Google API access token using the service account key
  const serviceAccountKey = Deno.env.get("GOOGLE_SERVICE_ACCOUNT_KEY");
  if (!serviceAccountKey) {
    return errorResponse("Server configuration error", 500);
  }

  try {
    const accessToken = await getGoogleAccessToken(serviceAccountKey);

    const verifyUrl = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${PACKAGE_NAME}/purchases/subscriptionsv2/tokens/${purchase_token}`;

    const response = await fetch(verifyUrl, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });

    if (!response.ok) {
      const errBody = await response.text();
      console.error("Google Play API error:", errBody);
      return errorResponse("Failed to verify purchase", 502);
    }

    const data = await response.json();

    // subscriptionState: SUBSCRIPTION_STATE_ACTIVE | SUBSCRIPTION_STATE_EXPIRED | ...
    const isActive = data.subscriptionState === "SUBSCRIPTION_STATE_ACTIVE" ||
      data.subscriptionState === "SUBSCRIPTION_STATE_IN_GRACE_PERIOD";

    const expiryTimeMs = data.lineItems?.[0]?.expiryTime
      ? new Date(data.lineItems[0].expiryTime).getTime()
      : null;

    return jsonResponse({ active: isActive, expiry_time_ms: expiryTimeMs });
  } catch (err) {
    console.error("Subscription verification error:", err);
    return errorResponse("Verification failed", 500);
  }
});

async function getGoogleAccessToken(serviceAccountKeyJson: string): Promise<string> {
  const key = JSON.parse(serviceAccountKeyJson);

  const now = Math.floor(Date.now() / 1000);
  const payload = {
    iss: key.client_email,
    scope: "https://www.googleapis.com/auth/androidpublisher",
    aud: "https://oauth2.googleapis.com/token",
    exp: now + 3600,
    iat: now,
  };

  // Encode JWT header and payload
  const header = btoa(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const payloadEncoded = btoa(JSON.stringify(payload));
  const signingInput = `${header}.${payloadEncoded}`;

  // Import the private key and sign
  const privateKey = await crypto.subtle.importKey(
    "pkcs8",
    pemToArrayBuffer(key.private_key),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );

  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", privateKey, new TextEncoder().encode(signingInput));
  const jwt = `${signingInput}.${btoa(String.fromCharCode(...new Uint8Array(signature)))}`;

  const tokenResponse = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=${jwt}`,
  });

  const tokenData = await tokenResponse.json();
  return tokenData.access_token;
}

function pemToArrayBuffer(pem: string): ArrayBuffer {
  const base64 = pem
    .replace("-----BEGIN PRIVATE KEY-----", "")
    .replace("-----END PRIVATE KEY-----", "")
    .replace(/\s/g, "");
  const binary = atob(base64);
  const buffer = new ArrayBuffer(binary.length);
  const view = new Uint8Array(buffer);
  for (let i = 0; i < binary.length; i++) view[i] = binary.charCodeAt(i);
  return buffer;
}
