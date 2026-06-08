package ma.portnet.authservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.portnet.authservice.dto.request.LoginRequest;
import ma.portnet.authservice.dto.request.LogoutRequest;
import ma.portnet.authservice.dto.request.RefreshTokenRequest;
import ma.portnet.authservice.dto.response.ApiResponse;
import ma.portnet.authservice.dto.response.LoginResponse;
import ma.portnet.authservice.service.AuthService;
import ma.portnet.authservice.service.KeycloakUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentification", description = "Login, refresh token, logout")
public class AuthController {

    private final AuthService          authService;
    private final KeycloakUserService  keycloakUserService;

    public AuthController(AuthService authService, KeycloakUserService keycloakUserService) {
        this.authService         = authService;
        this.keycloakUserService = keycloakUserService;
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────────
    @PostMapping("/login")
    @Operation(summary = "Connexion avec username et password")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Connexion réussie", response));
    }

    // ── REFRESH ───────────────────────────────────────────────────────────────
    @PostMapping("/refresh")
    @Operation(summary = "Renouveler le token d'accès")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        LoginResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.ok("Token renouvelé", response));
    }

    // ── LOGOUT ────────────────────────────────────────────────────────────────
    @PostMapping("/logout")
    @Operation(summary = "Déconnexion — révoque le token côté Keycloak")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) LogoutRequest request
    ) {
        String refreshToken = (request != null) ? request.refreshToken() : null;
        authService.logout(refreshToken);
        return ResponseEntity.ok(ApiResponse.ok("Déconnexion réussie", null));
    }

    // ── ROLES (admin) ─────────────────────────────────────────────────────────
    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Liste des rôles applicatifs disponibles")
    public ResponseEntity<ApiResponse<List<String>>> roles() {
        return ResponseEntity.ok(ApiResponse.ok(keycloakUserService.listAppRoles()));
    }
}