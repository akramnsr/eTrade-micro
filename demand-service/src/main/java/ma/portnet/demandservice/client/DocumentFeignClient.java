package ma.portnet.demandservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "document-service",
        url  = "${services.document-service-url}"
)
public interface DocumentFeignClient {

    @GetMapping("/api/v1/documents/check/{demandId}")
    Boolean hasRequiredDocuments(@PathVariable("demandId") String demandId);
}