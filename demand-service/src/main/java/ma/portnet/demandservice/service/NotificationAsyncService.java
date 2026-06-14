// demand-service/.../service/NotificationAsyncService.java
package ma.portnet.demandservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.portnet.demandservice.config.RabbitConstants;
import ma.portnet.demandservice.event.StatusChangeEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationAsyncService {

    private final RabbitTemplate rabbitTemplate;

    public void notifyStatusChange(String demandId, String exporterId,
                                   String newStatus, String recipientEmail) {
        try {
            StatusChangeEvent event = new StatusChangeEvent(
                    demandId, exporterId, newStatus, recipientEmail, LocalDateTime.now());
            rabbitTemplate.convertAndSend(
                    RabbitConstants.EXCHANGE, RabbitConstants.ROUTING_KEY, event);
            log.debug("Événement publié sur RabbitMQ : demande={} → {}", demandId, newStatus);
        } catch (Exception e) {
            log.warn("Publication RabbitMQ échouée (non-critique) : {}", e.getMessage());
        }
    }
}