package ma.portnet.demandservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.portnet.demandservice.client.NotificationFeignClient;
import ma.portnet.demandservice.dto.request.NotificationEventRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationAsyncService {

    private final NotificationFeignClient notificationFeignClient;

    @Async
    public void notifyStatusChange(String demandId,
                                   String exporterId,
                                   String newStatus) {
        try {
            NotificationEventRequest event = new NotificationEventRequest(
                    demandId,
                    exporterId,
                    newStatus,
                    LocalDateTime.now()
            );
            notificationFeignClient.sendEvent(event);
            log.debug("Notification envoyée pour demande {} → {}", demandId, newStatus);
        } catch (Exception e) {
            log.warn("Notification Service indisponible (non-critique): {}", e.getMessage());
        }
    }
}