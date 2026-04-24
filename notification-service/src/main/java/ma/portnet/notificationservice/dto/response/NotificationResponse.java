package ma.portnet.notificationservice.dto.response;

import ma.portnet.notificationservice.entity.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        String           notificationId,
        String           demandId,
        NotificationType notificationType,
        String           title,
        String           message,
        Boolean          readFlag,
        LocalDateTime    createdDate
) {}