package ma.portnet.authservice.dto.request;

public record UpdateUserRequest(
        String email,
        String fullName,
        String role,
        String password   // optionnel — null/vide = pas de changement
) {}