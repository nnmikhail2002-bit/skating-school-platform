--liquibase formatted sql

--changeset mikhail:004

CREATE TABLE trainers_services (

    trainer_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    FOREIGN KEY (trainer_id) REFERENCES trainers(id),
    FOREIGN KEY (service_id) REFERENCES school_services(id),
    PRIMARY KEY (trainer_id, service_id)

);