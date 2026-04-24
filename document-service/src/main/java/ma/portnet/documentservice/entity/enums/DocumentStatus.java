package ma.portnet.documentservice.entity.enums;

public enum DocumentStatus {
    UPLOADED,   // Uploadé — en attente de validation
    VALIDATED,  // Validé par la banque
    REJECTED    // Rejeté — à remplacer
}