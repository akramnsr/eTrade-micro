package ma.portnet.authservice.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import ma.portnet.authservice.dto.response.ApiResponse;
import ma.portnet.authservice.service.KeycloakUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/stats")
@Tag(name = "Admin Stats", description = "Statistiques système")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMINISTRATEUR')")
public class AdminStatsController {

    private final KeycloakUserService userService;

    public AdminStatsController(KeycloakUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("activeUsers",     userService.count());
        stats.put("totalOperations", 0);     // TODO: brancher demand-service via Feign
        stats.put("financedVolume",  0);     // TODO: brancher demand-service via Feign
        stats.put("currency",        "MAD");
        stats.put("activeServices",  6);
        stats.put("totalServices",   6);
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }
}