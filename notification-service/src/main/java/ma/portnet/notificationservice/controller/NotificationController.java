package ma.portnet.notificationservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import ma.portnet.notificationservice.dto.response.ApiResponse;
import ma.portnet.notificationservice.dto.response.NotificationResponse;
import ma.portnet.notificationservice.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Notifications in-app et événements")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // ── Reçoit un événement de demand-service (pas de token requis) ──

    @PostMapping("/events")
    @Operation(summary = "Recevoir un événement de changement de statut (inter-service)")
    public ResponseEntity<Void> receiveEvent(
            @RequestBody Map<String, Object> event
    ) {
        notificationService.processStatusChangeEvent(event);
        return ResponseEntity.ok().build();
    }

    // ── Endpoints pour le frontend ────────────────────────────

    @GetMapping
    @Operation(
            summary  = "Mes notifications",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(
            Authentication auth
    ) {
        String userId = extractUserId(auth);
        return ResponseEntity.ok(
                ApiResponse.ok(notificationService.getNotificationsForUser(userId))
        );
    }

    @PutMapping("/{id}/read")
    @Operation(
            summary  = "Marquer une notification comme lue",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable String id,
            Authentication auth
    ) {
        notificationService.markAsRead(id, extractUserId(auth));
        return ResponseEntity.ok(ApiResponse.ok("Notification marquée comme lue", null));
    }

    @PutMapping("/read-all")
    @Operation(
            summary  = "Marquer toutes les notifications comme lues",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(Authentication auth) {
        notificationService.markAllAsRead(extractUserId(auth));
        return ResponseEntity.ok(ApiResponse.ok("Toutes les notifications lues", null));
    }

    @GetMapping("/unread-count")
    @Operation(
            summary  = "Nombre de notifications non lues",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Long>> countUnread(Authentication auth) {
        long count = notificationService.countUnread(extractUserId(auth));
        return ResponseEntity.ok(ApiResponse.ok(count));
    }

    // ── Helper ────────────────────────────────────────────────

    private String extractUserId(Authentication auth) {
        return ((Jwt) auth.getPrincipal()).getSubject();
    }
}