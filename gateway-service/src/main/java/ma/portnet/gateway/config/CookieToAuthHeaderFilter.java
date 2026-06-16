package ma.portnet.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class CookieToAuthHeaderFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CookieToAuthHeaderFilter.class);
    private static final String ACCESS_COOKIE = "access_token";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Si un header Authorization est déjà présent, on ne touche à rien.
        if (request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            return chain.filter(exchange);
        }

        HttpCookie cookie = request.getCookies().getFirst(ACCESS_COOKIE);
        if (cookie != null && !cookie.getValue().isBlank()) {
            log.debug("[CookieFilter] Injection du Bearer pour {}", path);
            ServerHttpRequest mutated = request.mutate()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + cookie.getValue())
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        }

        // Normal sur les routes publiques (login/refresh) : pas d'alerte.
        log.debug("[CookieFilter] Pas de cookie access_token pour {}", path);
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}