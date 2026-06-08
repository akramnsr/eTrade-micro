package ma.portnet.authservice.service;

import jakarta.ws.rs.core.Response;
import ma.portnet.authservice.config.KeycloakAdminConfig;
import ma.portnet.authservice.dto.request.CreateUserRequest;
import ma.portnet.authservice.dto.request.UpdateUserRequest;
import ma.portnet.authservice.dto.response.UserResponse;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class KeycloakUserService {

    private static final List<String> APP_ROLES = List.of(
            "EXPORTATEUR", "BANQUE_EXPORTATEUR",
            "BANQUE_IMPORTATEUR", "ADMINISTRATEUR"
    );

    private final Keycloak            keycloak;
    private final KeycloakAdminConfig config;

    public KeycloakUserService(Keycloak keycloak, KeycloakAdminConfig config) {
        this.keycloak = keycloak;
        this.config   = config;
    }

    private RealmResource realm() {
        return keycloak.realm(config.getApplicationRealm());
    }

    // ── LIST ─────────────────────────────────────────────────────
    public List<UserResponse> listAll() {
        return realm().users().list().stream()
                .map(this::toDto)
                .toList();
    }

    public UserResponse getById(String id) {
        return toDto(realm().users().get(id).toRepresentation());
    }

    public long count() {
        return realm().users().count();
    }

    // ── CREATE ───────────────────────────────────────────────────
    public UserResponse create(CreateUserRequest req) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(req.username());
        user.setEmail(req.email());
        user.setFirstName(req.fullName());      // simple : tout le fullName dans firstName
        user.setEnabled(req.enabled() == null ? true : req.enabled());
        user.setEmailVerified(true);

        try (Response response = realm().users().create(user)) {
            if (response.getStatus() >= 400) {
                throw new RuntimeException(
                        "Échec création Keycloak (HTTP " + response.getStatus() +
                                "). L'email ou le username existe peut-être déjà."
                );
            }
            String userId = extractIdFromLocation(response.getLocation().getPath());

            if (req.password() != null && !req.password().isBlank()) {
                setPassword(userId, req.password());
            }
            if (req.role() != null && !req.role().isBlank()) {
                assignRealmRole(userId, req.role());
            }
            return getById(userId);
        }
    }

    // ── UPDATE ───────────────────────────────────────────────────
    public UserResponse update(String id, UpdateUserRequest req) {
        UserResource res = realm().users().get(id);
        UserRepresentation user = res.toRepresentation();

        if (req.email()    != null) user.setEmail(req.email());
        if (req.fullName() != null) user.setFirstName(req.fullName());
        res.update(user);

        if (req.password() != null && !req.password().isBlank()) {
            setPassword(id, req.password());
        }
        if (req.role() != null && !req.role().isBlank()) {
            replaceRealmRole(id, req.role());
        }
        return getById(id);
    }

    // ── DELETE ───────────────────────────────────────────────────
    public void delete(String id) {
        realm().users().get(id).remove();
    }

    // ── ENABLE / DISABLE ─────────────────────────────────────────
    public UserResponse setEnabled(String id, boolean enabled) {
        UserResource res = realm().users().get(id);
        UserRepresentation user = res.toRepresentation();
        user.setEnabled(enabled);
        res.update(user);
        return getById(id);
    }

    // ── ROLES ────────────────────────────────────────────────────
    public List<String> listAppRoles() {
        return realm().roles().list().stream()
                .map(RoleRepresentation::getName)
                .filter(APP_ROLES::contains)
                .toList();
    }

    // ── Helpers internes ─────────────────────────────────────────
    private void setPassword(String userId, String password) {
        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(password);
        cred.setTemporary(false);
        realm().users().get(userId).resetPassword(cred);
    }

    private void assignRealmRole(String userId, String roleName) {
        RoleRepresentation role = realm().roles().get(roleName).toRepresentation();
        realm().users().get(userId).roles().realmLevel()
                .add(Collections.singletonList(role));
    }

    private void replaceRealmRole(String userId, String newRoleName) {
        List<RoleRepresentation> currentAppRoles = realm().users().get(userId)
                .roles().realmLevel().listAll().stream()
                .filter(r -> APP_ROLES.contains(r.getName()))
                .toList();
        if (!currentAppRoles.isEmpty()) {
            realm().users().get(userId).roles().realmLevel().remove(currentAppRoles);
        }
        assignRealmRole(userId, newRoleName);
    }

    private String extractIdFromLocation(String locationPath) {
        return locationPath.substring(locationPath.lastIndexOf('/') + 1);
    }

    private UserResponse toDto(UserRepresentation u) {
        String role = "EXPORTATEUR";
        try {
            role = realm().users().get(u.getId()).roles().realmLevel().listAll().stream()
                    .map(RoleRepresentation::getName)
                    .filter(APP_ROLES::contains)
                    .findFirst()
                    .orElse("EXPORTATEUR");
        } catch (Exception ignored) {
            // si on n'arrive pas à lire les rôles, on garde le défaut
        }

        return new UserResponse(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getFirstName() != null ? u.getFirstName() : u.getUsername(),
                role,
                u.isEnabled() != null && u.isEnabled()
        );
    }
}