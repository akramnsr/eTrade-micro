-- V1__create_demand_tables.sql

CREATE TABLE IF NOT EXISTS demande_achat_traite (
                                                    demand_id               VARCHAR(36)  PRIMARY KEY,
    request_number          VARCHAR(50)  NOT NULL UNIQUE,
    created_date            TIMESTAMP    NOT NULL,
    submitted_date          TIMESTAMP,
    last_modified_date      TIMESTAMP,
    status                  VARCHAR(50)  NOT NULL,
    exporter_id             VARCHAR(36)  NOT NULL,
    bank_exporter_id        VARCHAR(36),
    montant_nominal         NUMERIC(18,2),
    devise                  VARCHAR(3),
    date_echeance           DATE,
    description             TEXT,
    is_ready_for_submission BOOLEAN DEFAULT FALSE
    );

CREATE TABLE IF NOT EXISTS detail_traite (
                                             draft_id          VARCHAR(36)  PRIMARY KEY,
    request_id        VARCHAR(36)  NOT NULL REFERENCES demande_achat_traite(demand_id),
    draft_number      VARCHAR(100),
    acceptor_name     VARCHAR(200),
    acceptor_country  VARCHAR(3),
    acceptor_bank     VARCHAR(200),
    acceptor_bank_aba VARCHAR(36),
    nominal_amount    NUMERIC(18,2),
    currency_code     VARCHAR(3),
    acceptance_date   DATE,
    maturity_date     DATE
    );

CREATE TABLE IF NOT EXISTS details_financiers (
                                                  financial_id           VARCHAR(36)  PRIMARY KEY,
    request_id             VARCHAR(36)  NOT NULL REFERENCES demande_achat_traite(demand_id),
    montant_nominal        NUMERIC(19,2),
    montant_net            NUMERIC(19,2),
    agios                  NUMERIC(19,2),
    frais_swift            NUMERIC(19,2),
    frais_courrier         NUMERIC(19,2),
    commission_negociation NUMERIC(19,2),
    prime_risque_pays      NUMERIC(19,2),
    taux_escompte          NUMERIC(10,6),
    interest_estimates     NUMERIC(19,2),
    date_financement       TIMESTAMP,
    payment_terms          VARCHAR(255)
    );

CREATE TABLE IF NOT EXISTS decision (
                                        decision_id            VARCHAR(36)  PRIMARY KEY,
    request_id             VARCHAR(36)  UNIQUE REFERENCES demande_achat_traite(demand_id),
    decision_type          VARCHAR(50),
    decision_date          TIMESTAMP,
    decision_by            VARCHAR(36),
    overall_risk_score     INTEGER,
    bank_rating_score      INTEGER,
    country_risk_score     INTEGER,
    country_risk_category  VARCHAR(50),
    documentary_risk_score INTEGER,
    conditions             TEXT,
    recommendation         TEXT
    );

CREATE TABLE IF NOT EXISTS historique_statuts (
                                                  history_id  VARCHAR(36)  PRIMARY KEY,
    demand_id   VARCHAR(36)  NOT NULL REFERENCES demande_achat_traite(demand_id),
    from_status VARCHAR(50),
    to_status   VARCHAR(50)  NOT NULL,
    changed_by  VARCHAR(36),
    change_date TIMESTAMP    NOT NULL,
    reason      VARCHAR(500),
    comment     TEXT
    );

CREATE TABLE IF NOT EXISTS partie_contrante (
                                                actor_id    VARCHAR(36)  PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    country     VARCHAR(3),
    bank_code   VARCHAR(20),
    swift_code  VARCHAR(11),
    is_bank     BOOLEAN DEFAULT FALSE
    );

CREATE TABLE IF NOT EXISTS document (
                                        document_id     VARCHAR(36)  PRIMARY KEY,
    demand_id       VARCHAR(36)  NOT NULL REFERENCES demande_achat_traite(demand_id),
    document_type   VARCHAR(100),
    file_name       VARCHAR(255),
    file_size       BIGINT,
    mime_type       VARCHAR(100),
    uploaded_date   TIMESTAMP    DEFAULT NOW(),
    uploaded_by     VARCHAR(36),
    last_modified   TIMESTAMP,
    mandatory_flag  BOOLEAN DEFAULT FALSE,
    validated       BOOLEAN DEFAULT FALSE,
    validation_date TIMESTAMP
    );