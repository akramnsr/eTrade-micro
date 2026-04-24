CREATE TYPE type_document AS ENUM (
    'TRAITE_ACCEPTEE',
    'FACTURE_COMMERCIALE',
    'CONNAISSEMENT',
    'CERTIFICAT_INSPECTION',
    'LISTE_COLISAGE',
    'CERTIFICAT_ORIGINE',
    'AUTRE'
);

CREATE TABLE document (
                          document_id     VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
                          demand_id       VARCHAR(36) NOT NULL,          -- référence externe (demand-service)
                          document_type   type_document NOT NULL,
                          file_name       VARCHAR(500) NOT NULL,
                          file_size       BIGINT,
                          mime_type       VARCHAR(100),
                          storage_path    VARCHAR(1000),                 -- chemin MinIO
                          uploaded_date   TIMESTAMP DEFAULT NOW(),
                          uploaded_by     VARCHAR(36),                   -- référence externe (auth-service)
                          mandatory_flag  BOOLEAN DEFAULT FALSE,
                          validated       BOOLEAN DEFAULT FALSE,
                          validation_date TIMESTAMP
);

CREATE INDEX idx_doc_demand ON document(demand_id);
CREATE INDEX idx_doc_type   ON document(demand_id, document_type);