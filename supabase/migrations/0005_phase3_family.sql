-- Phase 3: Family Protection Mode
-- family_sync_rules stores prefix rules and preferences synced between paired devices.
-- No user identity is stored — only the HMAC-SHA256 of the pairing token.

CREATE TABLE IF NOT EXISTS family_sync_rules (
    token_hash   TEXT        NOT NULL,
    rule_type    TEXT        NOT NULL CHECK (rule_type IN ('prefix', 'preference')),
    rule_payload JSONB       NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (token_hash, rule_type)
);

-- family_pairing stores active pairing tokens with expiry.
-- After the dependent scans the QR, the record is kept alive until unpaired.
CREATE TABLE IF NOT EXISTS family_pairing (
    token_hash   TEXT        PRIMARY KEY,
    expires_at   TIMESTAMPTZ NOT NULL,    -- 10-min window for initial scan
    paired_at    TIMESTAMPTZ,             -- null until dependent completes pairing
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Phase 3 reputation hardening: flags abnormally spiking numbers for review.
CREATE TABLE IF NOT EXISTS reputation_flags (
    number_hash  TEXT        PRIMARY KEY,
    reason       TEXT        NOT NULL,   -- "spike" | "oscillation" | "low_trust"
    flagged_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved     BOOLEAN     NOT NULL DEFAULT false
);

-- Row Level Security — no user identity, access keyed on token_hash only.
ALTER TABLE family_sync_rules ENABLE ROW LEVEL SECURITY;
ALTER TABLE family_pairing    ENABLE ROW LEVEL SECURITY;
ALTER TABLE reputation_flags  ENABLE ROW LEVEL SECURITY;

-- Service role bypasses RLS (Edge Functions use service role key).
-- Anonymous/authenticated roles have no direct access.
CREATE POLICY "service_only" ON family_sync_rules
    USING (false);  -- deny all; Edge Functions use service role which bypasses RLS

CREATE POLICY "service_only" ON family_pairing
    USING (false);

CREATE POLICY "service_only" ON reputation_flags
    USING (false);

-- Index for token_hash lookups on sync rules
CREATE INDEX IF NOT EXISTS idx_family_sync_token ON family_sync_rules (token_hash);

-- Auto-purge expired unpaired sessions (runs via pg_cron or manual cleanup)
-- Paired sessions are only removed by explicit unpair action.
CREATE INDEX IF NOT EXISTS idx_family_pairing_expires ON family_pairing (expires_at)
    WHERE paired_at IS NULL;
