--liquibase formatted sql

--changeset mikhail:012-create-outbox-events

CREATE TABLE outbox_events (
                               id UUID PRIMARY KEY,

                               aggregate_type VARCHAR(100) NOT NULL,
                               aggregate_id VARCHAR(100) NOT NULL,

                               event_type VARCHAR(100) NOT NULL,
                               payload TEXT NOT NULL,

                               created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               published_at TIMESTAMP WITH TIME ZONE,

                               attempts INT NOT NULL DEFAULT 0,
                               next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               last_error TEXT
);

CREATE INDEX idx_outbox_events_pending
    ON outbox_events (published_at, next_attempt_at, created_at);