-- Extension UUID
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Enum types
CREATE TYPE type_document AS ENUM (
    'TRAITE_ACCEPTEE',
    'FACTURE_COMMERCIALE',
    'CONNAISSEMENT',
    'CERTIFICAT_INSPECTION',
    'LISTE_COLISAGE',
    'CERTIFICAT_ORIGINE',
    'AUTRE'
);

CREATE TYPE document_status AS ENUM (
    'UPLOADED',
    'VALIDATED',
    'REJECTED'
);

-- Table principale des documents
CREATE TABLE document (
                          document_id     VARCHAR(36)     PRIMARY KEY DEFAULT gen_random_uuid()::text,
                          demand_id       VARCHAR(36)     NOT NULL,
                          document_type   type_document   NOT NULL,
                          file_name       VARCHAR(500)    NOT NULL,
                          original_name   VARCHAR(500)    NOT NULL,
                          file_size       BIGINT,
                          mime_type       VARCHAR(100),
                          storage_path    VARCHAR(1000),
                          uploaded_date   TIMESTAMP       NOT NULL DEFAULT NOW(),
                          uploaded_by     VARCHAR(36)     NOT NULL,
                          mandatory_flag  BOOLEAN         NOT NULL DEFAULT FALSE,
                          status          document_status NOT NULL DEFAULT 'UPLOADED',
                          validated_by    VARCHAR(36),
                          validation_date TIMESTAMP,
                          rejection_reason VARCHAR(500)
);

-- Index
CREATE INDEX idx_doc_demand_id   ON document(demand_id);
CREATE INDEX idx_doc_type        ON document(demand_id, document_type);
CREATE INDEX idx_doc_mandatory   ON document(demand_id, mandatory_flag);
CREATE INDEX idx_doc_uploaded_by ON document(uploaded_by);