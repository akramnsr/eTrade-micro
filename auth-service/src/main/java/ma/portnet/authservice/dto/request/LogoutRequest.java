package ma.portnet.authservice.dto.request;

/**
 * LogoutRequest
 *
 * Corps optionnel de la requête de logout.
 * Le refreshToken permet de révoquer la session côté Keycloak.
 * Si absent, le logout est purement local (le frontend vide son store).
 */
public record LogoutRequest(
        String refreshToken
) {}