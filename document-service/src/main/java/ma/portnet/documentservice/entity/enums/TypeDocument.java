package ma.portnet.documentservice.entity.enums;

public enum TypeDocument {
    TRAITE_ACCEPTEE,       // Obligatoire
    FACTURE_COMMERCIALE,   // Obligatoire
    CONNAISSEMENT,         // Obligatoire (B/L)
    CERTIFICAT_INSPECTION, // Optionnel
    LISTE_COLISAGE,        // Optionnel
    CERTIFICAT_ORIGINE,    // Optionnel
    AUTRE                  // Optionnel
}