INSERT INTO utilisateur (user_id, username, email, full_name, role, company_name, is_active)
VALUES
    ('u-exp-001',  'exportateur',  'exp@test.ma',   'Youssef Exportateur', 'EXPORTATEUR',       'AGRO EXPORT MAROC', true),
    ('u-bexp-001', 'banque-exp',   'bexp@test.ma',  'Fatima Banque Exp',   'BANQUE_EXPORTATEUR','CIH Banque',        true),
    ('u-bimp-001', 'banque-imp',   'bimp@test.ma',  'Hassan Banque Imp',   'BANQUE_IMPORTATEUR','Attijariwafa Bank', true),
    ('u-adm-001',  'admin-etrade', 'admin@test.ma', 'Admin eTrade',        'ADMINISTRATEUR',    'PortNet',           true);