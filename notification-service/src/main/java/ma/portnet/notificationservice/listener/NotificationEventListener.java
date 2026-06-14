// notification-service/.../listener/NotificationEventListener.java
package ma.portnet.notificationservice.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.portnet.notificationservice.config.RabbitConstants;
import ma.portnet.notificationservice.event.StatusChangeEvent;
import ma.portnet.notificationservice.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitConstants.QUEUE)
    public void onStatusChange(StatusChangeEvent event) {
        log.info("Événement RabbitMQ reçu : demande={} → {}",
                event.demandId(), event.newStatus());

        Map<String, Object> payload = new HashMap<>();
        payload.put("demandId",       event.demandId());
        payload.put("exporterId",     event.exporterId());
        payload.put("newStatus",      event.newStatus());
        payload.put("recipientEmail", event.recipientEmail());   // ← Option C

        notificationService.processStatusChangeEvent(payload);
    }
}