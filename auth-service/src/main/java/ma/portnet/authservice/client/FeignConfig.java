package ma.portnet.authservice.client;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Configuration
public class FeignConfig {

    /**
     * Propage le JWT de l'admin courant vers les appels Feign sortants.
     * Le token entrant (admin1) est forward tel quel à demand-service.
     */
    @Bean
    public RequestInterceptor jwtPropagationInterceptor() {
        return template -> {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();
                template.header("Authorization", "Bearer " + jwt.getTokenValue());
            }
        };
    }
}