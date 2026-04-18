-- liquibase formatted sql
-- changeset a.tikholoz:20260417000004

UPDATE oauth2_registered_client
SET client_secret                 = '$2b$10$T4dPAUnYKL49U4bOhVb6M.Hi6P4HUf4w.uWfk4tZllFBNJkebaggW',
    client_authentication_methods = 'client_secret_basic'
WHERE client_id = 'mcp-client';
