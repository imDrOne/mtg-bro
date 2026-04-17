-- liquibase formatted sql
-- changeset a.tikholoz:20260417000004

UPDATE oauth2_registered_client
SET client_secret                 = NULL,
    client_authentication_methods = 'none'
WHERE client_id = 'mcp-client';
