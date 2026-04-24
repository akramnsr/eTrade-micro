package ma.portnet.authservice.dto.response;

import java.util.List;

public record LoginResponse(
        String       accessToken,
        String       refreshToken,
        String       tokenType,
        Long         expiresIn,
        String       username,
        String       email,
        String       fullName,
        List<String> roles
) {}