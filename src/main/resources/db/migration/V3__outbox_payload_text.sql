-- Compatibility for environments that applied earlier V2 with JSONB payload.
ALTER TABLE outbox_event
  ALTER COLUMN payload TYPE TEXT USING payload::text;
