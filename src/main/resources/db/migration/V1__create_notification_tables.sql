CREATE TYPE notification_type AS ENUM (
    'STATUS_CHANGE', 'DOCUMENT_REQUIRED', 'APPROVAL', 'REJECTION', 'FINANCEMENT'
);

CREATE TABLE notification (
                              notification_id   VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
                              demand_id         VARCHAR(36) NOT NULL,
                              recipient_id      VARCHAR(36) NOT NULL,    -- référence externe auth-service
                              notification_type notification_type NOT NULL,
                              title             VARCHAR(200) NOT NULL,
                              message           TEXT NOT NULL,
                              read_flag         BOOLEAN DEFAULT FALSE,
                              created_date      TIMESTAMP DEFAULT NOW(),
                              sent_date         TIMESTAMP
);

CREATE INDEX idx_notif_recipient ON notification(recipient_id);
CREATE INDEX idx_notif_demand    ON notification(demand_id);
CREATE INDEX idx_notif_read      ON notification(recipient_id, read_flag);