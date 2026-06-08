package ma.portnet.demandservice.client;

import ma.portnet.demandservice.dto.request.NotificationEventRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "notification-service",
        url  = "${services.notification-service-url}"
)
public interface NotificationFeignClient {

    @PostMapping("/api/v1/notifications/events")
    void sendEvent(@RequestBody NotificationEventRequest event);
}