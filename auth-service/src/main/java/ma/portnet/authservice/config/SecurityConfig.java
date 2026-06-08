package ma.portnet.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final String[] PUBLIC_URLS = {
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout",
            "/actuator/**",
            "/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s ->
                        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .anyRequest().authenticated()
                )
                // Validation JWT pour les routes admin (users, etc.)
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter()))
                );
        return http.build();
    }

    /**
     * Convertit les rôles Keycloak (realm_access.roles) en authorities Spring
     * avec le préfixe ROLE_ pour que hasRole('ADMINISTRATEUR') fonctionne.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return converter;
    }

    static class KeycloakRealmRoleConverter
            implements Converter<Jwt, Collection<GrantedAuthority>> {

        private static final String CLIENT_ID = "etrade-backend";

        private final JwtGrantedAuthoritiesConverter defaultConverter =
                new JwtGrantedAuthoritiesConverter();

        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Collection<GrantedAuthority> all = new java.util.ArrayList<>();

            Collection<GrantedAuthority> defaults = defaultConverter.convert(jwt);
            if (defaults != null) all.addAll(defaults);

            // ── 1. realm_access.roles (rôles assignés directement au user) ──
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null) {
                @SuppressWarnings("unchecked")
                Collection<String> roles =
                        (Collection<String>) realmAccess.getOrDefault("roles", List.of());
                roles.stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                        .forEach(all::add);
            }

            // ── 2. resource_access["etrade-backend"].roles (rôles client) ──
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> clientRoles =
                        (Map<String, Object>) resourceAccess.get(CLIENT_ID);
                if (clientRoles != null) {
                    @SuppressWarnings("unchecked")
                    Collection<String> roles =
                            (Collection<String>) clientRoles.getOrDefault("roles", List.of());
                    roles.stream()
                            .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                            .forEach(all::add);
                }
            }

            return all;
        }
    }
}