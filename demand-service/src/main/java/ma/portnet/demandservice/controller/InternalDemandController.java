// demand-service/.../controller/InternalDemandController.java
package ma.portnet.demandservice.controller;

import ma.portnet.demandservice.service.DemandService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoints appelés par document-service (inter-service, sans token user).
 * DS-05 : document-service vérifie que la demande est modifiable, puis
 * enregistre la référence du document sur la demande.
 */
@RestController
@RequestMapping("/api/v1/demands")
public class InternalDemandController {

    private final DemandService demandService;

    public InternalDemandController(DemandService demandService) {
        this.demandService = demandService;
    }

    @GetMapping("/{id}/modifiable")
    public ResponseEntity<Boolean> isModifiable(@PathVariable String id) {
        return ResponseEntity.ok(demandService.isModifiable(id));
    }

    @PostMapping("/{id}/document-refs")
    public ResponseEntity<Void> register(@PathVariable String id,
                                         @RequestBody Map<String, Object> body) {
        demandService.registerDocumentReference(
                id,
                (String) body.get("documentId"),
                (String) body.get("documentType"),
                Boolean.TRUE.equals(body.get("mandatory"))
        );
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/document-refs/{documentId}")
    public ResponseEntity<Void> remove(@PathVariable String id,
                                       @PathVariable String documentId) {
        demandService.removeDocumentReference(id, documentId);
        return ResponseEntity.ok().build();
    }
}