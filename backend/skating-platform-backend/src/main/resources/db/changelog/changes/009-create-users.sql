--liquibase formatted sql

--changeset mikhail:009

CREATE TABLE users (
                          id BIGSERIAL PRIMARY KEY,
                          password VARCHAR(255) NOT NULL,
                          role VARCHAR(30) NOT NULL,
                          email VARCHAR(255) UNIQUE NOT NULL,
                          active BOOLEAN NOT NULL DEFAULT TRUE
);