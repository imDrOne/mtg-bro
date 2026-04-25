-- liquibase formatted sql
-- changeset a.tikholoz:20260425000006

CREATE TABLE api_permissions (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE role_api_permissions (
    role            VARCHAR(50)  NOT NULL,
    permission_id   BIGINT       NOT NULL REFERENCES api_permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role, permission_id)
);

CREATE INDEX idx_role_api_permissions_role ON role_api_permissions (role);

-- Seed permissions (naming: api:<resource>:<action>)
INSERT INTO api_permissions (name, description) VALUES
    ('api:cards:search',      'Search cards in collection'),
    ('api:cards:tribal',      'Analyze tribal depth for a creature type'),
    ('api:collection:view',   'View collection overview'),
    ('api:collection:import', 'Import collection from file'),
    ('api:collection:convert','Convert collection between formats'),
    ('api:decks:read',        'View decks'),
    ('api:decks:write',       'Create and manage decks'),
    ('api:scryfall:search',   'Proxy search to Scryfall API'),
    ('api:articles:read',     'Read Draftsim articles'),
    ('api:articles:parse',    'Trigger Draftsim article parsing'),
    ('api:stats:collect',     'Collect wizard stats from 17lands');

-- Grant all permissions to all roles (currently all authenticated users have equal access)
INSERT INTO role_api_permissions (role, permission_id)
SELECT r.role, p.id
FROM (VALUES ('FREE'), ('PRO'), ('ADMIN')) AS r(role)
CROSS JOIN api_permissions p
ON CONFLICT DO NOTHING;
