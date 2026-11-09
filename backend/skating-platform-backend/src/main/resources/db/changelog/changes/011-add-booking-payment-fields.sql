--liquibase formatted sql

--changeset mikhail:011

ALTER TABLE bookings
    ADD COLUMN amount NUMERIC(10, 2),
    ADD COLUMN currency VARCHAR(3),
    ADD COLUMN payment_attempt_id UUID;

UPDATE bookings b
SET amount = ss.price,
    currency = 'RUB'
    FROM training_sessions ts
JOIN school_services ss ON ss.id = ts.service_id
WHERE b.training_session_id = ts.id;

ALTER TABLE bookings
    ALTER COLUMN amount SET NOT NULL,
ALTER COLUMN currency SET NOT NULL;
