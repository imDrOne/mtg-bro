-- liquibase formatted sql

-- changeset auth-service:20260412000002
-- comment: create_oauth2_tables

-- Standard Spring Authorization Server schema for PostgreSQL

CREATE TABLE oauth2_registered_client
(
    id                            varchar(100)  NOT NULL,
    client_id                     varchar(100)  NOT NULL,
    client_id_issued_at           timestamptz   NOT NULL DEFAULT now(),
    client_secret                 varchar(200)  DEFAULT NULL,
    client_secret_expires_at      timestamptz   DEFAULT NULL,
    client_name                   varchar(200)  NOT NULL,
    client_authentication_methods varchar(1000) NOT NULL,
    authorization_grant_types     varchar(1000) NOT NULL,
    redirect_uris                 varchar(1000) DEFAULT NULL,
    post_logout_redirect_uris     varchar(1000) DEFAULT NULL,
    scopes                        varchar(1000) NOT NULL,
    client_settings               varchar(2000) NOT NULL,
    token_settings                varchar(2000) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE oauth2_authorization
(
    id                            varchar(100)  NOT NULL,
    registered_client_id          varchar(100)  NOT NULL,
    principal_name                varchar(200)  NOT NULL,
    authorization_grant_type      varchar(100)  NOT NULL,
    authorized_scopes             varchar(1000) DEFAULT NULL,
    attributes                    text          DEFAULT NULL,
    state                         varchar(500)  DEFAULT NULL,
    authorization_code_value      text          DEFAULT NULL,
    authorization_code_issued_at  timestamptz   DEFAULT NULL,
    authorization_code_expires_at timestamptz   DEFAULT NULL,
    authorization_code_metadata   text          DEFAULT NULL,
    access_token_value            text          DEFAULT NULL,
    access_token_issued_at        timestamptz   DEFAULT NULL,
    access_token_expires_at       timestamptz   DEFAULT NULL,
    access_token_metadata         text          DEFAULT NULL,
    access_token_type             varchar(100)  DEFAULT NULL,
    access_token_scopes           varchar(1000) DEFAULT NULL,
    oidc_id_token_value           text          DEFAULT NULL,
    oidc_id_token_issued_at       timestamptz   DEFAULT NULL,
    oidc_id_token_expires_at      timestamptz   DEFAULT NULL,
    oidc_id_token_metadata        text          DEFAULT NULL,
    refresh_token_value           text          DEFAULT NULL,
    refresh_token_issued_at       timestamptz   DEFAULT NULL,
    refresh_token_expires_at      timestamptz   DEFAULT NULL,
    refresh_token_metadata        text          DEFAULT NULL,
    device_code_value             text          DEFAULT NULL,
    device_code_issued_at         timestamptz   DEFAULT NULL,
    device_code_expires_at        timestamptz   DEFAULT NULL,
    device_code_metadata          text          DEFAULT NULL,
    user_code_value               text          DEFAULT NULL,
    user_code_issued_at           timestamptz   DEFAULT NULL,
    user_code_expires_at          timestamptz   DEFAULT NULL,
    user_code_metadata            text          DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE oauth2_authorization_consent
(
    registered_client_id varchar(100)  NOT NULL,
    principal_name       varchar(200)  NOT NULL,
    authorities          varchar(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);

-- rollback DROP TABLE oauth2_authorization_consent; DROP TABLE oauth2_authorization; DROP TABLE oauth2_registered_client;

-- changeset auth-service:20260412000003
-- comment: seed_mcp_oauth_client
-- NOTE: Replace the client_secret value with the BCrypt hash (without {bcrypt} prefix).
--       Generate: BCryptPasswordEncoder().encode("<your-secret>")
--       The actual secret is stored in GitHub Secrets as MCP_CLIENT_SECRET (production-auth-service env).
--       JdbcRegisteredClientRepository wraps it with {bcrypt} internally when matching.
--
-- NOTE: client_settings and token_settings keys MUST use full Spring setting names
--       (e.g. "settings.client.require-proof-key", not "requireProofKey").
--       These match the format produced by JdbcRegisteredClientRepository serialization.

INSERT INTO oauth2_registered_client (
    id,
    client_id,
    client_id_issued_at,
    client_secret,
    client_name,
    client_authentication_methods,
    authorization_grant_types,
    redirect_uris,
    scopes,
    client_settings,
    token_settings
) VALUES (
    'mcp-client-001',
    'mcp-client',
    now(),
    '$2b$10$T4dPAUnYKL49U4bOhVb6M.Hi6P4HUf4w.uWfk4tZllFBNJkebaggW',
    'MCP Server',
    'client_secret_basic',
    'authorization_code,refresh_token',
    'https://claude.ai/api/mcp/auth_callback',
    'openid,profile,decks:read',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":true,"settings.client.require-authorization-consent":false}',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000],"settings.token.access-token-time-to-live":["java.time.Duration",3600.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"},"settings.token.device-code-time-to-live":["java.time.Duration",300.000000000],"settings.token.reuse-refresh-tokens":false,"settings.token.refresh-token-time-to-live":["java.time.Duration",2592000.000000000],"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"]}'
);

-- rollback DELETE FROM oauth2_registered_client WHERE id = 'mcp-client-001';
