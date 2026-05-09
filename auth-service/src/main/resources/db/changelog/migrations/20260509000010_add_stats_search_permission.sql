-- liquibase formatted sql

-- changeset auth-service:20260509000010
INSERT INTO api_permissions (name, description)
VALUES ('api:stats:search', 'Search limited card stats');

INSERT INTO role_api_permissions (role, permission_id)
SELECT r.role, p.id
FROM (VALUES ('PRO'), ('ADMIN')) AS r(role)
JOIN api_permissions p ON p.name = 'api:stats:search';
