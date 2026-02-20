-- Auto-purge old reputation records (12 months with no reports)
-- Run as a scheduled job in Supabase (pg_cron or Edge Function cron)

CREATE OR REPLACE FUNCTION purge_stale_reputation()
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    DELETE FROM reputation
    WHERE last_reported_at < now() - INTERVAL '12 months';
END;
$$;
