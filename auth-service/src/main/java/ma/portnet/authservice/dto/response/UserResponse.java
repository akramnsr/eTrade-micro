package ma.portnet.authservice.dto.response;

public record UserResponse(
        String id,
        String username,
        String email,
        String fullName,
        String role,
        boolean enabled
) {}