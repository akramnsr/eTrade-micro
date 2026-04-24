-- 1) Ajouter la colonne si absente
ALTER TABLE decision
    ADD COLUMN IF NOT EXISTS request_id VARCHAR(36);

-- 2) (optionnel) si 1-1 : garantir unicité
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_decision_request_id'
    ) THEN
ALTER TABLE decision
    ADD CONSTRAINT uk_decision_request_id UNIQUE (request_id);
END IF;
END $$;

-- 3) FK vers la table des demandes (adapte le nom table/colonne si différent)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_decision_request'
    ) THEN
ALTER TABLE decision
    ADD CONSTRAINT fk_decision_request
        FOREIGN KEY (request_id) REFERENCES demande_achat_traite(request_id);
END IF;
END $$;