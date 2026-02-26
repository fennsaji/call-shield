-- Phase 3: Reputation hardening
-- Flags abnormally spiking numbers for review.

CREATE TABLE IF NOT EXISTS reputation_flags (
    number_hash  TEXT        PRIMARY KEY,
    reason       TEXT        NOT NULL,   -- "spike" | "oscillation" | "low_trust"
    flagged_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved     BOOLEAN     NOT NULL DEFAULT false
);

ALTER TABLE reputation_flags ENABLE ROW LEVEL SECURITY;

-- Service role bypasses RLS (Edge Functions use service role key).
-- Anonymous/authenticated roles have no direct access.
CREATE POLICY "service_only" ON reputation_flags
    USING (false);
