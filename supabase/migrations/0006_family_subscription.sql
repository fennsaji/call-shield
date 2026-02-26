-- Phase 3 (abuse prevention): Track guardian identity and subscription state per pairing.
-- Allows bulk revocation and server-side expiry when a guardian's plan lapses.

ALTER TABLE family_pairing
  ADD COLUMN IF NOT EXISTS guardian_device_hash  TEXT,
  ADD COLUMN IF NOT EXISTS plan_type             TEXT NOT NULL DEFAULT 'family_annual',
  ADD COLUMN IF NOT EXISTS subscription_expires_at TIMESTAMPTZ,    -- NULL = lifetime, never expires
  ADD COLUMN IF NOT EXISTS subscription_active   BOOLEAN NOT NULL DEFAULT TRUE;

-- Index for bulk revoke/renew operations scoped to a single guardian device.
CREATE INDEX IF NOT EXISTS idx_family_pairing_guardian
  ON family_pairing(guardian_device_hash)
  WHERE subscription_active = TRUE;

-- Existing rows inherit:
--   subscription_active = TRUE  (treated as lifetime — no disruption to legacy pairings)
--   subscription_expires_at = NULL (no expiry check applied)
