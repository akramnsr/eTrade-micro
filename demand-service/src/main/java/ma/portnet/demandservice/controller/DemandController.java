// demand-service/.../controller/DemandController.java
package ma.portnet.demandservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.portnet.demandservice.dto.request.CreateDemandRequest;
import ma.portnet.demandservice.dto.request.UpdateDemandRequest;
import ma.portnet.demandservice.dto.response.ApiResponse;
import ma.portnet.demandservice.dto.response.DemandResponse;
import ma.portnet.demandservice.dto.response.DemandSummaryResponse;
import ma.portnet.demandservice.service.DemandService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/demands")
@Tag(name = "Demands", description = "Export Bill Purchase — gestion des demandes")
@SecurityRequirement(name = "bearerAuth")
public class DemandController {

    private final DemandService demandService;

    public DemandController(DemandService demandService) {
        this.demandService = demandService;
    }

    // ── CRUD ──────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('EXPORTATEUR')")
    @Operation(summary = "Créer une demande EBP en DRAFT")
    public ResponseEntity<ApiResponse<DemandResponse>> create(
            @Valid @RequestBody CreateDemandRequest request,
            Authentication auth
    ) {
        String exporterId    = extractUserId(auth);
        String exporterEmail = extractEmail(auth);
        DemandResponse demand = demandService.createDemand(request, exporterId, exporterEmail);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Demande créée", demand));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir le détail d'une demande")
    public ResponseEntity<ApiResponse<DemandResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(demandService.getDemandById(id)));
    }

    @GetMapping
    @Operation(summary = "Lister les demandes (filtrées par rôle)")
    public ResponseEntity<ApiResponse<List<DemandSummaryResponse>>> list(
            @RequestParam(required = false) String status,
            Authentication auth
    ) {
        String userId = extractUserId(auth);
        List<String> roles = extractRoles(auth);
        return ResponseEntity.ok(ApiResponse.ok(demandService.listDemands(userId, roles, status)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('EXPORTATEUR')")
    @Operation(summary = "Modifier une demande (seulement si DRAFT)")
    public ResponseEntity<ApiResponse<DemandResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateDemandRequest request,
            Authentication auth
    ) {
        String exporterId = extractUserId(auth);
        return ResponseEntity.ok(ApiResponse.ok(demandService.updateDemand(id, request, exporterId)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EXPORTATEUR') or hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Supprimer une demande (DRAFT pour exportateur, tout pour admin)")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String id,
            Authentication auth
    ) {
        demandService.deleteDemand(id, extractUserId(auth), extractRoles(auth));
        return ResponseEntity.ok(ApiResponse.ok("Demande supprimée", null));
    }

    @PostMapping("/{id}/force-status")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Forcer un changement de statut (admin uniquement)")
    public ResponseEntity<ApiResponse<DemandResponse>> forceStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            Authentication auth
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                demandService.forceStatus(id, extractUserId(auth), body.get("status"), body.get("reason"))
        ));
    }

    // ── TRANSITIONS DE STATUT ─────────────────────────────────

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('EXPORTATEUR')")
    @Operation(summary = "Soumettre la demande — DRAFT → SUBMITTED")
    public ResponseEntity<ApiResponse<DemandResponse>> submit(
            @PathVariable String id, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(demandService.submitDemand(id, extractUserId(auth))));
    }

    @PostMapping("/{id}/start-analysis")
    @PreAuthorize("hasRole('BANQUE_EXPORTATEUR')")
    @Operation(summary = "Démarrer l'analyse — SUBMITTED → IN_ANALYSIS")
    public ResponseEntity<ApiResponse<DemandResponse>> startAnalysis(
            @PathVariable String id, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(demandService.startAnalysis(id, extractUserId(auth))));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('BANQUE_EXPORTATEUR')")
    @Operation(summary = "Approuver — IN_ANALYSIS → APPROVED")
    public ResponseEntity<ApiResponse<DemandResponse>> approve(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication auth) {
        String conditions = body != null ? body.getOrDefault("conditions", "") : "";
        return ResponseEntity.ok(ApiResponse.ok(demandService.approveDemand(id, extractUserId(auth), conditions)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('BANQUE_EXPORTATEUR')")
    @Operation(summary = "Rejeter — IN_ANALYSIS → REJECTED")
    public ResponseEntity<ApiResponse<DemandResponse>> reject(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(demandService.rejectDemand(id, extractUserId(auth), body.get("reason"))));
    }

    @PostMapping("/{id}/finance")
    @PreAuthorize("hasRole('BANQUE_EXPORTATEUR')")
    @Operation(summary = "Financer — APPROVED → FINANCED")
    public ResponseEntity<ApiResponse<DemandResponse>> finance(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication auth) {
        String transferRef = body != null ? body.get("transferReference") : null;
        java.time.LocalDate valueDate = null;
        if (body != null && body.get("valueDate") != null && !body.get("valueDate").isBlank()) {
            valueDate = java.time.LocalDate.parse(body.get("valueDate"));
        }
        return ResponseEntity.ok(ApiResponse.ok(
                demandService.financeDemand(id, extractUserId(auth), transferRef, valueDate)));
    }

    @PostMapping("/{id}/present")
    @PreAuthorize("hasRole('BANQUE_EXPORTATEUR')")
    @Operation(summary = "Présenter à échéance — FINANCED → PRESENTATION_AT_MATURITY")
    public ResponseEntity<ApiResponse<DemandResponse>> present(
            @PathVariable String id, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(demandService.presentDemand(id, extractUserId(auth))));
    }

    @PostMapping("/{id}/settle")
    @PreAuthorize("hasRole('BANQUE_IMPORTATEUR')")
    @Operation(summary = "Régler — PRESENTATION_AT_MATURITY → SETTLED")
    public ResponseEntity<ApiResponse<DemandResponse>> settle(
            @PathVariable String id, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(demandService.settleDemand(id, extractUserId(auth))));
    }

    // ── CALCUL FINANCIER (aperçu) ─────────────────────────────

    @GetMapping("/calculate")
    @Operation(summary = "Calculer les agios et montant net (aperçu)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculate(
            @RequestParam java.math.BigDecimal nominal,
            @RequestParam java.math.BigDecimal taux,
            @RequestParam int jours) {
        return ResponseEntity.ok(ApiResponse.ok(demandService.calculatePreview(nominal, taux, jours)));
    }

    // ── Helpers ───────────────────────────────────────────────

    private String extractUserId(Authentication auth) {
        return ((Jwt) auth.getPrincipal()).getSubject();
    }

    private String extractEmail(Authentication auth) {
        return (String) ((Jwt) auth.getPrincipal()).getClaims().get("email");
    }

    private List<String> extractRoles(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .toList();
    }
}