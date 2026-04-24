CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE notification_type AS ENUM (
    'STATUS_CHANGE', 'DOCUMENT_REQUIRED', 'APPROVAL',
    'REJECTION', 'FINANCEMENT', 'PRESENTATION', 'SETTLEMENT', 'INFO'
);

CREATE TABLE notification (
                              notification_id   VARCHAR(36)       PRIMARY KEY DEFAULT gen_random_uuid()::text,
                              demand_id         VARCHAR(36)       NOT NULL,
                              recipient_id      VARCHAR(36)       NOT NULL,
                              notification_type notification_type NOT NULL,
                              title             VARCHAR(200)      NOT NULL,
                              message           TEXT              NOT NULL,
                              read_flag         BOOLEAN           NOT NULL DEFAULT FALSE,
                              created_date      TIMESTAMP         NOT NULL DEFAULT NOW(),
                              sent_date         TIMESTAMP
);

CREATE INDEX idx_notif_recipient      ON notification(recipient_id);
CREATE INDEX idx_notif_unread         ON notification(recipient_id, read_flag);
CREATE INDEX idx_notif_demand         ON notification(demand_id);
CREATE INDEX idx_notif_created        ON notification(created_date DESC);