import { SupabaseClient } from "https://esm.sh/@supabase/supabase-js@2";

// In-memory rate limit store for same edge worker instance.
// For distributed rate limiting, use a Redis layer (Phase 3+).
const rateLimitStore = new Map<string, { count: number; windowStart: number }>();

/**
 * Simple in-memory rate limiter.
 * Returns true if the request is allowed, false if rate-limited.
 */
export function checkRateLimit(
  key: string,
  maxRequests: number,
  windowSeconds: number,
): boolean {
  const now = Date.now();
  const windowMs = windowSeconds * 1000;

  const entry = rateLimitStore.get(key);

  if (!entry || now - entry.windowStart > windowMs) {
    rateLimitStore.set(key, { count: 1, windowStart: now });
    return true;
  }

  if (entry.count >= maxRequests) {
    return false;
  }

  entry.count++;
  return true;
}
