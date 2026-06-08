package ma.portnet.demandservice.dto.request;

import java.time.LocalDateTime;

public record NotificationEventRequest(
        String        demandId,
        String        exporterId,
        String        newStatus,
        LocalDateTime timestamp
) {}