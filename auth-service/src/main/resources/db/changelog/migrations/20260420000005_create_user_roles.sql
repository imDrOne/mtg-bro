-- liquibase formatted sql
-- changeset a.tikholoz:20260420000005

CREATE TABLE user_roles (
    user_id    BIGINT      NOT NULL REFERENCES users(id),
    role       VARCHAR(50) NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role)
);

CREATE INDEX idx_user_roles_user_id ON user_roles (user_id);

-- Assign FREE to all existing users (idempotent)
INSERT INTO user_roles (user_id, role)
SELECT id, 'FREE'
FROM users
ON CONFLICT DO NOTHING;
