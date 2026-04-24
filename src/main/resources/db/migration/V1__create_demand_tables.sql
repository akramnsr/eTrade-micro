CREATE TYPE status_demande AS ENUM (
    'DRAFT', 'SUBMITTED', 'IN_ANALYSIS', 'APPROVED',
    'REJECTED', 'FINANCED', 'PRESENTATION_AT_MATURITY', 'SETTLED'
);

CREATE TYPE type_decision AS ENUM (
    'APPROVE', 'REJECT', 'REQUEST_MORE_INFO', 'REQUEST_ADDITIONAL_GUARANTEE'
);

CREATE TYPE categorie_risque_pays AS ENUM (
    'CATEGORY_0', 'CATEGORY_1', 'CATEGORY_2', 'CATEGORY_3'
);

-- IMPORTANT : exporter_id est une String — pas de FK vers auth DB
CREATE TABLE demand_achat_traite (
                                     demand_id               VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
                                     request_number          VARCHAR(50) NOT NULL UNIQUE,
                                     created_date            TIMESTAMP NOT NULL DEFAULT NOW(),
                                     submitted_date          TIMESTAMP,
                                     last_modified_date      TIMESTAMP,
                                     status                  status_demande NOT NULL DEFAULT 'DRAFT',
                                     exporter_id             VARCHAR(36) NOT NULL,   -- référence externe auth-service
                                     bank_exporter_id        VARCHAR(36),            -- référence externe auth-service
                                     montant_nominal         NUMERIC(18,2),
                                     devise                  VARCHAR(3),
                                     date_echeance           DATE,
                                     description             TEXT,
                                     is_ready_for_submission BOOLEAN DEFAULT FALSE
);

CREATE TABLE detail_traite (
                               draft_id          VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
                               request_id        VARCHAR(36) NOT NULL REFERENCES demand_achat_traite(demand_id),
                               draft_number      VARCHAR(100),
                               acceptor_country  VARCHAR(3),
                               acceptor_name     VARCHAR(200),
                               acceptor_bank     VARCHAR(200),
                               nominal_amount    NUMERIC(18,2),
                               currency_code     VARCHAR(3),
                               acceptance_date   DATE,
                               maturity_date     DATE
);

CREATE TABLE details_financiers (
                                    financial_id            VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
                                    request_id              VARCHAR(36) NOT NULL UNIQUE REFERENCES demand_achat_traite(demand_id),
                                    montant_nominal         NUMERIC(18,2),
                                    montant_net             NUMERIC(18,2),
                                    agios                   NUMERIC(18,2),
                                    frais_swift             NUMERIC(18,2) DEFAULT 30,
                                    frais_courrier          NUMERIC(18,2) DEFAULT 100,
                                    commission_negociation  NUMERIC(18,2),
                                    taux_escompte           NUMERIC(8,4),
                                    date_financement        TIMESTAMP
);

CREATE TABLE decision (
                          decision_id             VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
                          request_id              VARCHAR(36) NOT NULL REFERENCES demand_achat_traite(demand_id),
                          decision_type           type_decision,
                          decision_date           TIMESTAMP,
                          decision_by             VARCHAR(36),            -- référence externe auth-service
                          overall_risk_score      INTEGER,
                          bank_rating_score       INTEGER,
                          country_risk_score      INTEGER,
                          documentary_risk_score  INTEGER,
                          conditions              TEXT,
                          recommendation          TEXT
);

CREATE TABLE historique_statuts (
                                    history_id   VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
                                    demand_id    VARCHAR(36) NOT NULL REFERENCES demand_achat_traite(demand_id),
                                    from_status  status_demande,
                                    to_status    status_demande NOT NULL,
                                    changed_by   VARCHAR(36),                       -- référence externe auth-service
                                    change_date  TIMESTAMP NOT NULL DEFAULT NOW(),
                                    reason       VARCHAR(500),
                                    comment      TEXT
);

-- Index de performance
CREATE INDEX idx_demand_exporter ON demand_achat_traite(exporter_id);
CREATE INDEX idx_demand_status   ON demand_achat_traite(status);
CREATE INDEX idx_hist_demand     ON historique_statuts(demand_id);