CREATE TABLE IF NOT EXISTS transfers (
  token_hash TEXT PRIMARY KEY,
  object_key TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  expires_at INTEGER NOT NULL,
  consumed_at INTEGER,
  byte_size INTEGER NOT NULL CHECK(byte_size BETWEEN 1 AND 2097152)
);
CREATE INDEX IF NOT EXISTS idx_transfers_expiry ON transfers(expires_at);
