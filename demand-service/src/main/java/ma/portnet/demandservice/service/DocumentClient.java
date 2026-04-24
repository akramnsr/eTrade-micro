package ma.portnet.demandservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class DocumentClient {

    private final RestTemplate restTemplate;

    @Value("${services.document-service-url}")
    private String documentUrl;

    public DocumentClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Vérifie que les documents obligatoires sont uploadés avant le submit
     * Appel synchrone — bloque si Document Service est down
     */
    public boolean hasRequiredDocuments(String demandId) {
        try {
            Boolean result = restTemplate.getForObject(
                    documentUrl + "/api/v1/documents/check/" + demandId,
                    Boolean.class
            );
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Document Service indisponible pour demande {}: {}", demandId, e.getMessage());
            // Si Document Service est down, bloquer le submit par sécurité
            throw new RuntimeException("Service de documents indisponible, impossible de valider");
        }
    }
}