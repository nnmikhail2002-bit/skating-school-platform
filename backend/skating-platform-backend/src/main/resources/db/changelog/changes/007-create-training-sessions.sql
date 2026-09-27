--liquibase formatted sql

--changeset mikhail:007

CREATE TABLE training_sessions (
                          id BIGSERIAL PRIMARY KEY,
                          trainer_id BIGINT NOT NULL,
                          service_id BIGINT NOT NULL,
                          start_time TIMESTAMP NOT NULL,
                          end_time TIMESTAMP NOT NULL,
                          status VARCHAR(30) NOT NULL,
                          capacity INTEGER NOT NULL ,
                          FOREIGN KEY (trainer_id) REFERENCES trainers(id),
                          FOREIGN KEY (service_id) REFERENCES school_services(id),
                          CHECK (capacity > 0)
);
