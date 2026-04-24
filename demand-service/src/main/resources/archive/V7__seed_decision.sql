INSERT INTO decision (numero, statut)
VALUES ('DEC-0001', 'BROUILLON')
    ON CONFLICT (numero) DO NOTHING;