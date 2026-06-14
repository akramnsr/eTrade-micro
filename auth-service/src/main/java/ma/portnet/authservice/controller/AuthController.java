package ma.portnet.authservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import ma.portnet.authservice.dto.request.LoginRequest;
import ma.portnet.authservice.dto.request.RefreshTokenRequest;
import ma.portnet.authservice.dto.response.ApiResponse;
import ma.portnet.authservice.dto.response.LoginResponse;
import ma.portnet.authservice.service.AuthService;
import ma.portnet.authservice.service.KeycloakUserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentification", description = "Login, refresh token, logout")
public class AuthController {

    private static final String ACCESS_COOKIE  = "access_token";
    private static final String REFRESH_COOKIE = "refresh_token";

    private final AuthService         authService;
    private final KeycloakUserService keycloakUserService;

    public AuthController(AuthService authService, KeycloakUserService keycloakUserService) {
        this.authService         = authService;
        this.keycloakUserService = keycloakUserService;
    }

    // ── LOGIN ─────────────────────────────────────────────────
    @PostMapping("/login")
    @Operation(summary = "Connexion — dépose les jetons dans des cookies HttpOnly")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildAccessCookie(response.accessToken(), response.expiresIn()).toString())
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(response.refreshToken()).toString())
                .body(ApiResponse.ok(sanitize(response)));
    }

    // ── REFRESH ───────────────────────────────────────────────
    @PostMapping("/refresh")
    @Operation(summary = "Renouvelle les jetons via le cookie refresh_token")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(HttpServletRequest httpRequest) {
        String refreshToken = readCookie(httpRequest, REFRESH_COOKIE);
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(401).body(ApiResponse.error("Session expirée"));
        }
        LoginResponse response = authService.refresh(new RefreshTokenRequest(refreshToken));

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildAccessCookie(response.accessToken(), response.expiresIn()).toString())
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(response.refreshToken()).toString())
                .body(ApiResponse.ok(sanitize(response)));
    }

    // ── LOGOUT ────────────────────────────────────────────────
    @PostMapping("/logout")
    @Operation(summary = "Déconnexion — supprime les cookies")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest httpRequest) {
        String refreshToken = readCookie(httpRequest, REFRESH_COOKIE);
        authService.logout(refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie(ACCESS_COOKIE).toString())
                .header(HttpHeaders.SET_COOKIE, clearCookie(REFRESH_COOKIE).toString())
                .body(ApiResponse.ok(null));
    }

    // ── ROLES (admin) ────────────────────────────────────────
    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Liste des rôles applicatifs disponibles")
    public ResponseEntity<ApiResponse<List<String>>> roles() {
        return ResponseEntity.ok(ApiResponse.ok(keycloakUserService.listAppRoles()));
    }

    // ── HELPERS COOKIES ──────────────────────────────────────
    private ResponseCookie buildAccessCookie(String token, long expiresInSeconds) {
        return ResponseCookie.from(ACCESS_COOKIE, token)
                .httpOnly(true)
                .secure(false)          // ⚠️ true en prod HTTPS
                .sameSite("Lax")
                .path("/")              // ← CRUCIAL : racine
                .maxAge(Duration.ofSeconds(expiresInSeconds))
                .build();
    }

    private ResponseCookie buildRefreshCookie(String token) {
        return ResponseCookie.from(REFRESH_COOKIE, token)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(7))
                .build();
    }

    private ResponseCookie clearCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }

    private String readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (var c : request.getCookies()) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }

    /** Retire les jetons du corps — le frontend ne reçoit que le non-sensible */
    private LoginResponse sanitize(LoginResponse r) {
        return new LoginResponse(
                null, null, r.tokenType(), r.expiresIn(),
                r.username(), r.email(), r.fullName(), r.roles()
        );
    }
}