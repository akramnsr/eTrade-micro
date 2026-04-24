-- sécuriser la contrainte PK sur decision_id
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_name = 'decision'
          AND constraint_type = 'PRIMARY KEY'
    ) THEN
ALTER TABLE decision ADD PRIMARY KEY (decision_id);
END IF;
END$$;