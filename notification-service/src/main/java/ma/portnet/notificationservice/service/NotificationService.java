package ma.portnet.notificationservice.service;

import lombok.extern.slf4j.Slf4j;
import ma.portnet.notificationservice.dto.response.NotificationResponse;
import ma.portnet.notificationservice.entity.Notification;
import ma.portnet.notificationservice.entity.enums.NotificationType;
import ma.portnet.notificationservice.exception.ResourceNotFoundException;
import ma.portnet.notificationservice.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    public NotificationService(NotificationRepository notificationRepository,
                               EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
    }

    // ── Reçoit un événement de demand-service ─────────────────

    @Transactional
    public void processStatusChangeEvent(Map<String, Object> event) {
        String demandId   = (String) event.get("demandId");
        String exporterId = (String) event.get("exporterId");
        String newStatus  = (String) event.get("newStatus");
        String recipientEmail = (String) event.get("recipientEmail"); // peut être null

        if (demandId == null || exporterId == null || newStatus == null) {
            log.warn("Événement notification incomplet : {}", event);
            return;
        }

        NotificationType type    = resolveType(newStatus);
        String           title   = buildTitle(newStatus);
        String           message = buildMessage(demandId, newStatus);

        // Notification in-app
        Notification notification = Notification.builder()
                .notificationId(UUID.randomUUID().toString())
                .demandId(demandId)
                .recipientId(exporterId)
                .notificationType(type)
                .title(title)
                .message(message)
                .readFlag(false)
                .createdDate(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);

        // Email (DS-08/09/10 : "Envoie un email à l'exportateur")
        emailService.send(recipientEmail, title, message);

        log.info("Notification créée : demandId={} status={} pour {}",
                demandId, newStatus, exporterId);
    }

    // ── Lire les notifications d'un utilisateur ────────────────

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsForUser(String userId) {
        return notificationRepository
                .findByRecipientIdOrderByCreatedDateDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Marquer comme lue ─────────────────────────────────────

    @Transactional
    public void markAsRead(String notificationId, String userId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Notification introuvable : " + notificationId)
                );

        if (!n.getRecipientId().equals(userId)) {
            throw new SecurityException("Accès non autorisé à cette notification");
        }

        n.setReadFlag(true);
        notificationRepository.save(n);
    }

    // ── Marquer toutes comme lues ─────────────────────────────

    @Transactional
    public void markAllAsRead(String userId) {
        List<Notification> unread = notificationRepository
                .findByRecipientIdAndReadFlagFalseOrderByCreatedDateDesc(userId);
        unread.forEach(n -> n.setReadFlag(true));
        notificationRepository.saveAll(unread);
    }

    // ── Compter les non-lues ──────────────────────────────────

    @Transactional(readOnly = true)
    public long countUnread(String userId) {
        return notificationRepository.countByRecipientIdAndReadFlagFalse(userId);
    }

    // ── Helpers privés ────────────────────────────────────────

    private NotificationType resolveType(String status) {
        return switch (status.toUpperCase()) {
            case "APPROVED"                -> NotificationType.APPROVAL;
            case "REJECTED"                -> NotificationType.REJECTION;
            case "FINANCED"                -> NotificationType.FINANCEMENT;
            case "PRESENTATION_AT_MATURITY"-> NotificationType.PRESENTATION;
            case "SETTLED"                 -> NotificationType.SETTLEMENT;
            default                        -> NotificationType.STATUS_CHANGE;
        };
    }

    private String buildTitle(String status) {
        return switch (status.toUpperCase()) {
            case "SUBMITTED"               -> "Demande soumise avec succès";
            case "IN_ANALYSIS"             -> "Demande en cours d'analyse";
            case "APPROVED"                -> "Demande approuvée ✓";
            case "REJECTED"                -> "Demande rejetée";
            case "FINANCED"                -> "Financement accordé — fonds versés ✓";
            case "PRESENTATION_AT_MATURITY"-> "Traite présentée à échéance";
            case "SETTLED"                 -> "Opération réglée avec succès ✓";
            default                        -> "Mise à jour de votre demande";
        };
    }

    private String buildMessage(String demandId, String status) {
        return switch (status.toUpperCase()) {
            case "SUBMITTED"   ->
                    "Votre demande " + demandId + " a été soumise à la banque pour analyse.";
            case "IN_ANALYSIS" ->
                    "Votre demande " + demandId + " est en cours d'analyse par la banque.";
            case "APPROVED"    ->
                    "Votre demande " + demandId + " a été approuvée. Le financement sera versé prochainement.";
            case "REJECTED"    ->
                    "Votre demande " + demandId + " a été rejetée. Consultez les détails pour plus d'informations.";
            case "FINANCED"    ->
                    "Les fonds de votre demande " + demandId + " ont été versés sur votre compte.";
            case "PRESENTATION_AT_MATURITY" ->
                    "La traite de votre demande " + demandId + " a été présentée à la banque importatrice.";
            case "SETTLED"     ->
                    "Le règlement de votre demande " + demandId + " est confirmé. Opération clôturée.";
            default            ->
                    "Le statut de votre demande " + demandId + " a été mis à jour : " + status;
        };
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getNotificationId(),
                n.getDemandId(),
                n.getNotificationType(),
                n.getTitle(),
                n.getMessage(),
                n.getReadFlag(),
                n.getCreatedDate()
        );
    }
}