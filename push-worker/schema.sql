CREATE TABLE IF NOT EXISTS devices (
  id INTEGER PRIMARY KEY CHECK(id = 1),
  token TEXT NOT NULL,
  updated_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS events (
  event_key TEXT PRIMARY KEY,
  sent INTEGER NOT NULL DEFAULT 0,
  created_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_events_created_at ON events(created_at);
