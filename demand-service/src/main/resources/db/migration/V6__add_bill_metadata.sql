-- V_NEXT__add_bill_metadata.sql
ALTER TABLE demande_achat_traite
    ADD COLUMN IF NOT EXISTS bill_context    VARCHAR(50);

ALTER TABLE demande_achat_traite
    ADD COLUMN IF NOT EXISTS lc_dc_reference VARCHAR(50);

ALTER TABLE demande_achat_traite
    ADD COLUMN IF NOT EXISTS purchase_type   VARCHAR(30);