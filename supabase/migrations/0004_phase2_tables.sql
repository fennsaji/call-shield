-- Phase 2 schema additions
-- quarantine_queue: numbers whose rapid-report velocity warrants human review before score is acted upon
-- number_categories: per-category vote tallies for each reported number

-- ─────────────────────────────────────────────────────────────────────────────
-- quarantine_queue
-- Numbers that received ≥5 reports within a 24-hour window enter quarantine.
-- Their confidence scores are held at or below HIGH_CONFIDENCE_THRESHOLD (0.8)
-- until manually reviewed or until the burst expires (48h TTL).
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS quarantine_queue (
    number_hash         TEXT        PRIMARY KEY,
    trigger_reason      TEXT        NOT NULL DEFAULT 'velocity',
    report_count_24h    INT         NOT NULL DEFAULT 0,
    quarantined_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at          TIMESTAMPTZ NOT NULL DEFAULT now() + INTERVAL '48 hours',
    reviewed            BOOLEAN     NOT NULL DEFAULT false
);

CREATE INDEX idx_quarantine_expires ON quarantine_queue (expires_at) WHERE reviewed = false;

-- ─────────────────────────────────────────────────────────────────────────────
-- number_categories
-- Vote tallies for each (number, category) pair.
-- Replaces the last-write-wins category on the reputation table with a proper
-- vote count. The dominant category (highest vote count) is propagated back to
-- reputation.category when the score is recomputed.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS number_categories (
    number_hash         TEXT        NOT NULL,
    category            TEXT        NOT NULL,
    vote_count          INT         NOT NULL DEFAULT 1,
    last_voted_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (number_hash, category)
);

CREATE INDEX idx_number_categories_number_hash ON number_categories (number_hash);

-- Purge expired quarantine entries (called by pg_cron daily)
CREATE OR REPLACE FUNCTION purge_expired_quarantine()
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    DELETE FROM quarantine_queue
    WHERE expires_at < now() AND reviewed = false;
END;
$$;
