-- Données de référence : parties contrantes (banques partenaires)
CREATE TABLE partie_contrante (
                                  actor_id         VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
                                  name             VARCHAR(200) NOT NULL,
                                  country          VARCHAR(3),
                                  bank_code        VARCHAR(20),
                                  swift_code       VARCHAR(11),
                                  is_bank          BOOLEAN DEFAULT FALSE
);

INSERT INTO partie_contrante (actor_id, name, country, bank_code, swift_code, is_bank)
VALUES
    ('pc-001', 'CIH Banque Maroc',       'MAR', 'CIH001', 'CIHMMAMC', true),
    ('pc-002', 'Attijariwafa Bank',       'MAR', 'ATT001', 'BCMAMAMC', true),
    ('pc-003', 'Deutsche Bank Frankfurt', 'DEU', 'DB0001', 'DEUTDEDB', true);