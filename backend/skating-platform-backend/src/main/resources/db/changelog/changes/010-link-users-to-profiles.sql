--liquibase formatted sql

--changeset mikhail:010

ALTER TABLE users
    ADD COLUMN student_id BIGINT UNIQUE,
    ADD COLUMN trainer_id BIGINT UNIQUE,
    ADD FOREIGN KEY (student_id) REFERENCES students(id),
    ADD FOREIGN KEY (trainer_id) REFERENCES trainers(id);