-- liquibase formatted sql

-- changeset a.tikholoz:20260524000001
UPDATE oauth2_registered_client
SET scopes = 'openid,profile,decks:read,offline_access'
WHERE client_id = 'mcp-client';

-- rollback UPDATE oauth2_registered_client SET scopes = 'openid,profile,decks:read' WHERE client_id = 'mcp-client';
