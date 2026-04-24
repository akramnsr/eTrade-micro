package ma.portnet.authservice.service;

import lombok.extern.slf4j.Slf4j;
import ma.portnet.authservice.config.KeycloakProperties;
import ma.portnet.authservice.dto.request.LoginRequest;
import ma.portnet.authservice.dto.request.RefreshTokenRequest;
import ma.portnet.authservice.dto.response.LoginResponse;
import ma.portnet.authservice.entity.Utilisateur;
import ma.portnet.authservice.exception.AuthenticationException;
import ma.portnet.authservice.repository.UtilisateurRepository;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AuthService {

    private final KeycloakProperties      keycloakProperties;
    private final ObjectMapper            objectMapper;
    private final RestTemplate            restTemplate;
    private final UtilisateurRepository   utilisateurRepository;

    public AuthService(KeycloakProperties    keycloakProperties,
                       ObjectMapper          objectMapper,
                       UtilisateurRepository utilisateurRepository) {
        this.keycloakProperties    = keycloakProperties;
        this.objectMapper          = objectMapper;
        this.restTemplate          = new RestTemplate();
        this.utilisateurRepository = utilisateurRepository;
    }

    // ── LOGIN ─────────────────────────────────────────────────

    public LoginResponse login(LoginRequest request) {
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type",    "password");
            form.add("client_id",     keycloakProperties.getClientId());
            form.add("client_secret", keycloakProperties.getClientSecret());
            form.add("username",      request.username());
            form.add("password",      request.password());
            form.add("scope",         "openid profile email");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    keycloakProperties.getTokenUrl(),
                    new HttpEntity<>(form, headers),
                    String.class
            );

            JsonNode json        = objectMapper.readTree(response.getBody());
            String accessToken   = json.get("access_token").asText();
            String refreshToken  = json.get("refresh_token").asText();
            long   expiresIn     = json.get("expires_in").asLong();

            Map<String, Object> claims   = decodeJwtPayload(accessToken);
            String              username = (String) claims.getOrDefault("preferred_username", "");
            String              email    = (String) claims.getOrDefault("email", "");
            String              fullName = (String) claims.getOrDefault("name", username);
            String              subject  = (String) claims.get("sub");
            List<String>        roles    = extractRoles(claims);

            // Mettre à jour la date de dernière connexion
            updateLastLogin(username, subject);

            log.info("Login réussi : {}", username);

            return new LoginResponse(
                    accessToken, refreshToken, "Bearer",
                    expiresIn, username, email, fullName, roles
            );

        } catch (HttpClientErrorException.Unauthorized e) {
            throw new AuthenticationException("Identifiant ou mot de passe incorrect");
        } catch (HttpClientErrorException e) {
            throw new AuthenticationException("Erreur d'authentification : " + e.getStatusCode());
        } catch (Exception e) {
            log.error("Erreur login : {}", e.getMessage());
            throw new AuthenticationException("Service d'authentification indisponible");
        }
    }

    // ── REFRESH ───────────────────────────────────────────────

    public LoginResponse refresh(RefreshTokenRequest request) {
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type",    "refresh_token");
            form.add("client_id",     keycloakProperties.getClientId());
            form.add("client_secret", keycloakProperties.getClientSecret());
            form.add("refresh_token", request.refreshToken());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    keycloakProperties.getTokenUrl(),
                    new HttpEntity<>(form, headers),
                    String.class
            );

            JsonNode json       = objectMapper.readTree(response.getBody());
            String accessToken  = json.get("access_token").asText();
            String refreshToken = json.get("refresh_token").asText();
            long   expiresIn    = json.get("expires_in").asLong();

            Map<String, Object> claims = decodeJwtPayload(accessToken);

            return new LoginResponse(
                    accessToken, refreshToken, "Bearer", expiresIn,
                    (String) claims.getOrDefault("preferred_username", ""),
                    (String) claims.getOrDefault("email", ""),
                    (String) claims.getOrDefault("name", ""),
                    extractRoles(claims)
            );

        } catch (HttpClientErrorException e) {
            throw new AuthenticationException("Session expirée, veuillez vous reconnecter");
        } catch (Exception e) {
            throw new AuthenticationException("Impossible de renouveler la session");
        }
    }

    // ── HELPERS PRIVÉS ────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> decodeJwtPayload(String token) {
        try {
            String[] parts   = token.split("\\.");
            String   payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            return objectMapper.readValue(payload, Map.class);
        } catch (Exception e) {
            throw new AuthenticationException("Token JWT invalide");
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Map<String, Object> claims) {
        try {
            Map<String, Object> realmAccess = (Map<String, Object>) claims.get("realm_access");
            if (realmAccess == null) return new ArrayList<>();
            List<String> all = (List<String>) realmAccess.get("roles");
            if (all == null) return new ArrayList<>();
            return all.stream()
                    .filter(r -> r.equals("EXPORTATEUR")
                            || r.equals("BANQUE_EXPORTATEUR")
                            || r.equals("BANQUE_IMPORTATEUR")
                            || r.equals("ADMINISTRATEUR"))
                    .toList();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void updateLastLogin(String username, String keycloakId) {
        utilisateurRepository.findByUsername(username).ifPresent(u -> {
            u.setLastLoginDate(LocalDateTime.now());
            if (u.getKeycloakId() == null && keycloakId != null) {
                u.setKeycloakId(keycloakId);
            }
            utilisateurRepository.save(u);
        });
    }
}