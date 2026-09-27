--liquibase formatted sql

--changeset mikhail:008

CREATE TABLE bookings
(
    id                  BIGSERIAL PRIMARY KEY,
    training_session_id BIGINT      NOT NULL,
    student_id          BIGINT      NOT NULL,
    booked_at           TIMESTAMP   NOT NULL,
    status              VARCHAR(30) NOT NULL,

    FOREIGN KEY (student_id) REFERENCES students (id),
    FOREIGN KEY (training_session_id) REFERENCES training_sessions (id),

    UNIQUE (student_id, training_session_id)
);
