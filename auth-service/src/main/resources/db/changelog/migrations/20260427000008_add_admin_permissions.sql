--liquibase formatted sql

--changeset auth-service:20260427000008
INSERT INTO api_permissions (name, description) VALUES
    ('api:admin:users:read',   'List and search users'),
    ('api:admin:users:create', 'Create admin users'),
    ('api:admin:users:block',  'Block and unblock users');

INSERT INTO role_api_permissions (role, permission_id)
SELECT 'ADMIN', id
FROM api_permissions
WHERE name IN ('api:admin:users:read', 'api:admin:users:create', 'api:admin:users:block');
