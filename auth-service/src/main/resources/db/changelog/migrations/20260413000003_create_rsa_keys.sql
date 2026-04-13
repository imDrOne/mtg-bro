-- liquibase formatted sql

-- changeset auth-service:20260413000003
-- comment: create_rsa_keys

CREATE TABLE rsa_keys
(
    id          smallint    PRIMARY KEY DEFAULT 1,
    key_id      varchar(36) NOT NULL,
    public_key  text        NOT NULL,
    private_key text        NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT rsa_keys_single_row CHECK (id = 1)
);

-- rollback DROP TABLE rsa_keys;
