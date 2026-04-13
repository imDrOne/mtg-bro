-- liquibase formatted sql

-- changeset auth-service:20260412000001
-- comment: create_users

CREATE TABLE users
(
    id            bigserial    PRIMARY KEY,
    email         varchar(255) NOT NULL,
    username      varchar(50)  NOT NULL,
    password_hash varchar(255) NOT NULL,
    enabled       boolean      NOT NULL DEFAULT true,
    created_at    timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT uq_users_email    UNIQUE (email),
    CONSTRAINT uq_users_username UNIQUE (username)
);

-- rollback DROP TABLE users;
