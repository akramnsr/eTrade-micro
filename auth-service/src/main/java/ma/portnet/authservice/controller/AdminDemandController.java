package ma.portnet.authservice.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import ma.portnet.authservice.client.DemandServiceClient;
import ma.portnet.authservice.dto.response.ApiResponse;
import ma.portnet.authservice.dto.response.DemandDetailDto;
import ma.portnet.authservice.dto.response.DemandSummaryDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/demands")
@Tag(name = "Admin Demands", description = "Supervision des demandes par l'administrateur")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMINISTRATEUR')")
public class AdminDemandController {

    private final DemandServiceClient demandClient;

    public AdminDemandController(DemandServiceClient demandClient) {
        this.demandClient = demandClient;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DemandSummaryDto>>> list(
            @RequestParam(required = false) String status
    ) {
        var resp = demandClient.listDemands(status);
        return ResponseEntity.ok(ApiResponse.ok(resp.data()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DemandDetailDto>> getById(@PathVariable String id) {
        var resp = demandClient.getDemand(id);
        return ResponseEntity.ok(ApiResponse.ok(resp.data()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        demandClient.deleteDemand(id);
        return ResponseEntity.ok(ApiResponse.ok("Demande supprimée", null));
    }

    @PostMapping("/{id}/force-status")
    public ResponseEntity<ApiResponse<DemandDetailDto>> forceStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body
    ) {
        var resp = demandClient.forceStatus(id, body);
        return ResponseEntity.ok(ApiResponse.ok("Statut forcé", resp.data()));
    }
}