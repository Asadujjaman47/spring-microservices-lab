-- Dedupe table: records every event_id we've successfully handled so that retries
-- (either broker redelivery or the upstream publishing twice) are no-ops.
CREATE TABLE processed_events (
    event_id     UUID         PRIMARY KEY,
    event_type   VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL
);