-- demand-service/src/main/resources/db/migration/V2__add_document_reference.sql
-- ⚠️ Renomme V100 avec le numéro qui suit ta dernière migration (ex: si tu en es à V2, mets V3)
CREATE TABLE document_reference (
                                    reference_id    VARCHAR(36)  NOT NULL,
                                    demand_id       VARCHAR(36)  NOT NULL,
                                    document_id     VARCHAR(36)  NOT NULL,
                                    document_type   VARCHAR(50)  NOT NULL,
                                    mandatory       BOOLEAN      NOT NULL DEFAULT FALSE,
                                    registered_date TIMESTAMP    NOT NULL,
                                    CONSTRAINT pk_document_reference PRIMARY KEY (reference_id),
                                    CONSTRAINT fk_docref_demand FOREIGN KEY (demand_id)
                                        REFERENCES demande_achat_traite (demand_id) ON DELETE CASCADE,
                                    CONSTRAINT uq_docref_demand_type UNIQUE (demand_id, document_type)
);