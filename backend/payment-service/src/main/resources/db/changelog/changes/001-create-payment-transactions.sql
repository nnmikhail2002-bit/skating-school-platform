--liquibase formatted sql

--changeset skating:001-create-payment-transactions

CREATE TABLE payment_transactions
(
    id                BIGSERIAL PRIMARY KEY,
    order_id          BIGINT         NOT NULL,
    amount            NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    currency          VARCHAR(3)     NOT NULL,
    status            VARCHAR(32)    NOT NULL,
    idempotency_key   VARCHAR(128)   NOT NULL,
    gateway_reference VARCHAR(128),
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_payment_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX idx_payment_order_id ON payment_transactions (order_id);
