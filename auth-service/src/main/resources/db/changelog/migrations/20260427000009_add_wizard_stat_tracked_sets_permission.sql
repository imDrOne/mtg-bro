-- liquibase formatted sql

-- changeset auth-service:20260427000009
INSERT INTO api_permissions (name, description) VALUES
    ('api:stats:tracked-sets:manage', 'Manage wizard stat tracked sets');

INSERT INTO role_api_permissions (role, permission_id)
SELECT 'ADMIN', id
FROM api_permissions
WHERE name = 'api:stats:tracked-sets:manage';
