-- CallShield Row Level Security Policies
-- Edge Functions are the ONLY entry point — clients never access tables directly.
-- RLS is a second line of defence.

ALTER TABLE reputation             ENABLE ROW LEVEL SECURITY;
ALTER TABLE report_events          ENABLE ROW LEVEL SECURITY;
ALTER TABLE reporter_deduplication ENABLE ROW LEVEL SECURITY;
ALTER TABLE seed_db_versions       ENABLE ROW LEVEL SECURITY;

-- No direct client access to any table.
-- All access is via Edge Functions using the service_role key.
-- The anon key has zero privileges.

CREATE POLICY "No direct client read on reputation"
    ON reputation FOR SELECT USING (false);

CREATE POLICY "No direct client insert on reputation"
    ON reputation FOR INSERT WITH CHECK (false);

CREATE POLICY "No direct client read on report_events"
    ON report_events FOR SELECT USING (false);

CREATE POLICY "No direct client insert on report_events"
    ON report_events FOR INSERT WITH CHECK (false);

CREATE POLICY "No direct client read on reporter_deduplication"
    ON reporter_deduplication FOR SELECT USING (false);

CREATE POLICY "No direct client insert on reporter_deduplication"
    ON reporter_deduplication FOR INSERT WITH CHECK (false);

-- seed_db_versions manifest is public-read (no sensitive data)
CREATE POLICY "Public read on seed_db_versions"
    ON seed_db_versions FOR SELECT USING (true);

CREATE POLICY "No direct client insert on seed_db_versions"
    ON seed_db_versions FOR INSERT WITH CHECK (false);
