ALTER TABLE decision
ALTER COLUMN decision_id TYPE VARCHAR(36)
  USING decision_id::text;ALTER TABLE decision ADD COLUMN IF NOT EXISTS bank_rating_score integer;
ALTER TABLE decision ADD COLUMN IF NOT EXISTS country_risk_score integer;
ALTER TABLE decision ADD COLUMN IF NOT EXISTS documentary_risk_score integer;
ALTER TABLE decision ADD COLUMN IF NOT EXISTS overall_risk_score integer;
ALTER TABLE decision ADD COLUMN IF NOT EXISTS decision_type varchar(255);
ALTER TABLE decision ADD COLUMN IF NOT EXISTS decision_date timestamp;
ALTER TABLE decision ADD COLUMN IF NOT EXISTS decision_by varchar(36);
ALTER TABLE decision ADD COLUMN IF NOT EXISTS country_risk_category varchar(255);
ALTER TABLE decision ADD COLUMN IF NOT EXISTS conditions text;
ALTER TABLE decision ADD COLUMN IF NOT EXISTS recommendation text;