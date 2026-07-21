package com.taskflow.gateway.filter;

import com.taskflow.gateway.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    public AuthenticationFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    public static class Config {
        // Marker config used by the gateway DSL.
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();
            if (path.contains("/auth/register") || path.contains("/auth/login")) {
                return chain.filter(exchange);
            }

            List<String> authHeaders = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION);
            if (authHeaders == null || authHeaders.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Authorization header");
            }

            String token = authHeaders.get(0);
            if (token != null && token.startsWith(BEARER_PREFIX)) {
                token = token.substring(BEARER_PREFIX.length());
            }

            try {
                jwtUtil.validateToken(token);
            } catch (Exception e) {
                log.warn("Rejected invalid JWT at gateway: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
            }
            return chain.filter(exchange);
        };
    }
}
