package ma.portnet.notificationservice.entity.enums;

public enum NotificationType {
    STATUS_CHANGE,        // Changement de statut d'une demande
    DOCUMENT_REQUIRED,    // Document obligatoire manquant
    APPROVAL,             // Demande approuvée
    REJECTION,            // Demande rejetée
    FINANCEMENT,          // Fonds versés
    PRESENTATION,         // Traite présentée à échéance
    SETTLEMENT,           // Règlement effectué
    INFO                  // Information générale
}