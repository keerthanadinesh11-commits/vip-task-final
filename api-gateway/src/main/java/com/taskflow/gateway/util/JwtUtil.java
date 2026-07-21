package com.taskflow.gateway.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;

/**
 * Gateway-level JWT utility — only validates the signature, does not decode claims.
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    public void validateToken(final String token) {
        Key key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }
}
