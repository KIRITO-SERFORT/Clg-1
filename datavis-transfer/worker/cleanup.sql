-- Run from a scheduled Worker/cron job. Also delete corresponding R2 objects before dropping rows.
DELETE FROM transfers WHERE expires_at <= unixepoch() OR consumed_at IS NOT NULL;
