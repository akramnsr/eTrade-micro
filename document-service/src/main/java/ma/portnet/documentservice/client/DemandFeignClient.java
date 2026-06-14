// document-service/.../client/DemandFeignClient.java
package ma.portnet.documentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Client Feign vers demand-service.
 *
 * IMPORTANT : `consumes` et `produces` doivent être positionnés explicitement
 * sur les @PostMapping qui ont un @RequestBody. Sans ça, Feign envoie le body
 * en form-urlencoded par défaut et Jackson côté demand-service tombe sur
 * "Unrecognized character escape ':' (code 58)" quand il essaie de parser
 * les UUID. Ce fix règle la callback DS-05 (enregistrement des références
 * de documents).
 */
@FeignClient(name = "demand-service", url = "${services.demand-service-url}")
public interface DemandFeignClient {

    @GetMapping("/api/v1/demands/{demandId}/modifiable")
    Boolean isModifiable(@PathVariable("demandId") String demandId);

    @PostMapping(
            value    = "/api/v1/demands/{demandId}/document-refs",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    void registerReference(@PathVariable("demandId") String demandId,
                           @RequestBody Map<String, Object> body);

    @DeleteMapping("/api/v1/demands/{demandId}/document-refs/{documentId}")
    void removeReference(@PathVariable("demandId") String demandId,
                         @PathVariable("documentId") String documentId);
}