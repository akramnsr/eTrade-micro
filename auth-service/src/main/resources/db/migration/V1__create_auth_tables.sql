CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE role_utilisateur AS ENUM (
    'EXPORTATEUR', 'BANQUE_EXPORTATEUR',
    'BANQUE_IMPORTATEUR', 'ADMINISTRATEUR'
);

CREATE TABLE utilisateur (
                             user_id         VARCHAR(36)      PRIMARY KEY DEFAULT gen_random_uuid()::text,
                             username        VARCHAR(100)     NOT NULL UNIQUE,
                             email           VARCHAR(200)     NOT NULL UNIQUE,
                             full_name       VARCHAR(200)     NOT NULL,
                             role            role_utilisateur NOT NULL,
                             company_name    VARCHAR(200),
                             company_number  VARCHAR(50),
                             keycloak_id     VARCHAR(36)      UNIQUE,
                             created_date    TIMESTAMP        NOT NULL DEFAULT NOW(),
                             last_login_date TIMESTAMP,
                             is_active       BOOLEAN          NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_user_username    ON utilisateur(username);
CREATE INDEX idx_user_keycloak_id ON utilisateur(keycloak_id);