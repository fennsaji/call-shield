-- CallShield Phase 1 Schema
-- All number identifiers are HMAC-SHA256 hashes — no raw phone numbers stored.

-- ─────────────────────────────────────────────────────────────────────────────
-- reputation
-- Derived reputation state, read-optimised. Recomputed from report_events.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS reputation (
    number_hash         TEXT        PRIMARY KEY,
    report_count        INT         NOT NULL DEFAULT 0,
    unique_reporters    INT         NOT NULL DEFAULT 0,
    confidence_score    FLOAT       NOT NULL DEFAULT 0.0,
    category            TEXT,                           -- most-reported category
    negative_signals    INT         NOT NULL DEFAULT 0, -- "Not Spam" corrections
    last_reported_at    TIMESTAMPTZ,
    last_computed_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─────────────────────────────────────────────────────────────────────────────
-- report_events
-- Append-only source of truth for all reports.
-- Enables full score recomputation from raw data.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS report_events (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    number_hash         TEXT        NOT NULL,
    device_token_hash   TEXT        NOT NULL,
    category            TEXT        NOT NULL,
    reported_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    schema_version      INT         NOT NULL DEFAULT 1
);

CREATE INDEX idx_report_events_number_hash ON report_events (number_hash);
CREATE INDEX idx_report_events_reported_at ON report_events (reported_at);

-- ─────────────────────────────────────────────────────────────────────────────
-- reporter_deduplication
-- Enforces true uniqueness of reporters per number.
-- A device may only contribute once to unique_reporters for any given number — ever.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS reporter_deduplication (
    number_hash         TEXT        NOT NULL,
    device_token_hash   TEXT        NOT NULL,
    first_reported_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (number_hash, device_token_hash)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- seed_db_versions
-- Tracks seed database versions available in Supabase Storage.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS seed_db_versions (
    version             INT         PRIMARY KEY,
    sha256_checksum     TEXT        NOT NULL,
    storage_path        TEXT        NOT NULL,   -- path in Supabase Storage bucket
    released_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_current          BOOLEAN     NOT NULL DEFAULT false
);

-- Only one version can be current
CREATE UNIQUE INDEX idx_seed_db_versions_current ON seed_db_versions (is_current) WHERE is_current = true;

-- ─────────────────────────────────────────────────────────────────────────────
-- NOTE: quarantine_queue and number_categories are Phase 2.
-- They are NOT created here. They will be added in a Phase 2 migration.
-- ─────────────────────────────────────────────────────────────────────────────
