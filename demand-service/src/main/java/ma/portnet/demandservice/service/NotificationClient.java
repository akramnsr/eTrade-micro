package ma.portnet.demandservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
public class NotificationClient {

    private final RestTemplate restTemplate;

    @Value("${services.notification-service-url}")
    private String notificationUrl;

    public NotificationClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Appel asynchrone — ne bloque jamais EBP si Notification est down
     */
    @Async
    public void notifyStatusChange(String demandId,
                                   String exporterId,
                                   String newStatus) {
        try {
            Map<String, Object> event = Map.of(
                    "demandId",   demandId,
                    "exporterId", exporterId,
                    "newStatus",  newStatus,
                    "timestamp",  LocalDateTime.now().toString()
            );
            restTemplate.postForEntity(
                    notificationUrl + "/api/v1/notifications/events",
                    event,
                    Void.class
            );
            log.debug("Notification envoyée pour demande {} → {}", demandId, newStatus);
        } catch (Exception e) {
            // Log mais ne jamais faire planter EBP
            log.warn("Notification Service indisponible (non-critique): {}", e.getMessage());
        }
    }
}