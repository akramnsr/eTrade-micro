package ma.portnet.authservice.dto.request;

public record CreateUserRequest(
        String username,
        String email,
        String fullName,
        String role,
        String password,
        Boolean enabled
) {}