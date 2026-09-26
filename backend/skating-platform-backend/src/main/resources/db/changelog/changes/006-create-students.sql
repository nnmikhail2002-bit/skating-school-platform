--liquibase formatted sql

--changeset mikhail:006

CREATE TABLE students (
                          id BIGSERIAL PRIMARY KEY,
                          first_name VARCHAR(255) NOT NULL,
                          last_name VARCHAR(255) NOT NULL,
                          phone VARCHAR(20) UNIQUE NOT NULL,
                          email VARCHAR(255),
                          active BOOLEAN NOT NULL DEFAULT TRUE
);
