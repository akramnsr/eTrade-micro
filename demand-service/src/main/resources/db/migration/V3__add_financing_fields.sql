-- demand-service/.../db/migration/V101__add_financing_fields.sql
ALTER TABLE demande_achat_traite ADD COLUMN transfer_reference VARCHAR(100);
ALTER TABLE demande_achat_traite ADD COLUMN value_date DATE;