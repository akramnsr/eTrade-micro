package ma.portnet.authservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import ma.portnet.authservice.dto.request.CreateUserRequest;
import ma.portnet.authservice.dto.request.LoginRequest;
import ma.portnet.authservice.dto.request.RefreshTokenRequest;
import ma.portnet.authservice.dto.response.ApiResponse;
import ma.portnet.authservice.dto.response.LoginResponse;
import ma.portnet.authservice.dto.response.UserResponse;
import ma.portnet.authservice.service.AuthService;
import ma.portnet.authservice.service.KeycloakUserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentification", description = "Login, refresh token, logout, inscription")
public class AuthController {

    private static final String ACCESS_COOKIE  = "access_token";
    private static final String REFRESH_COOKIE = "refresh_token";

    private final AuthService         authService;
    private final KeycloakUserService keycloakUserService;

    public AuthController(AuthService authService, KeycloakUserService keycloakUserService) {
        this.authService         = authService;
        this.keycloakUserService = keycloakUserService;
    }

    @PostMapping("/login")
    @Operation(summary = "Connexion — dépose les jetons dans des cookies HttpOnly")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildAccessCookie(response.accessToken(), response.expiresIn()).toString())
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(response.refreshToken()).toString())
                .body(ApiResponse.ok(sanitize(response)));
    }

    @PostMapping("/register")
    @Operation(summary = "Inscription publique d'un exportateur")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @RequestBody CreateUserRequest request
    ) {
        validateRegistration(request);

        CreateUserRequest safeRequest = new CreateUserRequest(
                request.username().trim(),
                request.email().trim(),
                request.fullName().trim(),
                "EXPORTATEUR",
                request.password(),
                true
        );

        UserResponse created = keycloakUserService.create(safeRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Compte exportateur créé", created));
    }

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

    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Liste des rôles applicatifs disponibles")
    public ResponseEntity<ApiResponse<List<String>>> roles() {
        return ResponseEntity.ok(ApiResponse.ok(keycloakUserService.listAppRoles()));
    }

    private void validateRegistration(CreateUserRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Données d'inscription manquantes");
        }

        if (request.username() == null || request.username().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'identifiant est obligatoire");
        }

        if (request.username().trim().length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'identifiant doit contenir au moins 3 caractères");
        }

        if (request.email() == null || request.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'email est obligatoire");
        }

        if (request.fullName() == null || request.fullName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom complet est obligatoire");
        }

        if (request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe est obligatoire");
        }

        if (request.password().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe doit contenir au moins 8 caractères");
        }
    }

    private ResponseCookie buildAccessCookie(String token, long expiresInSeconds) {
        return ResponseCookie.from(ACCESS_COOKIE, token)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
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

    private LoginResponse sanitize(LoginResponse r) {
        return new LoginResponse(
                null,
                null,
                r.tokenType(),
                r.expiresIn(),
                r.username(),
                r.email(),
                r.fullName(),
                r.roles()
        );
    }
}