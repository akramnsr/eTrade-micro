ALTER TABLE demande_achat_traite ADD COLUMN IF NOT EXISTS transfer_reference VARCHAR(100);
ALTER TABLE demande_achat_traite ADD COLUMN IF NOT EXISTS value_date         DATE;
ALTER TABLE demande_achat_traite ADD COLUMN IF NOT EXISTS exporter_email     VARCHAR(200);