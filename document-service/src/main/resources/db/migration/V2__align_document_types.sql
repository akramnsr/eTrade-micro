-- document-service/src/main/resources/db/migration/V4__align_document_types.sql
UPDATE document SET document_type = 'PACKING_LIST'
WHERE document_type = 'LISTE_COLISAGE';

UPDATE document SET document_type = 'CERTIFICAT_ORIGINE'
WHERE document_type = 'CERTIFICAT_INSPECTION';

-- 'AUTRE' n'a plus d'équivalent → on les supprime ou on les bascule vers TITRE_EXPORTATION
DELETE FROM document WHERE document_type = 'AUTRE';