-- liquibase formatted sql
-- changeset a.tikholoz:20260419000004

UPDATE oauth2_registered_client
SET client_authentication_methods = 'client_secret_basic,client_secret_post'
WHERE client_id = 'mcp-client';
