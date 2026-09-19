--liquibase formatted sql

--changeset mikhail:003

CREATE TABLE trainers (
                                 id BIGSERIAL PRIMARY KEY,
                                 first_name VARCHAR(255) NOT NULL,
                                 last_name VARCHAR(255) NOT NULL,
                                 phone VARCHAR(20) NOT NULL,
                                 experience_years INTEGER NOT NULL,
                                 active BOOLEAN NOT NULL DEFAULT TRUE
);