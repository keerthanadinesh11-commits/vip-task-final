package com.taskflow.chatservice.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    private static final String SECRET =
            "taskflowsupersecretkeytaskflowsupersecretkeytaskflowsupersecretkey1234";

    private JwtAuthFilter filter;

    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter();
        ReflectionTestUtils.setField(filter, "secret", SECRET);
        SecurityContextHolder.clearContext();
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private String buildToken(String username, String role, String email) {
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        Map<String, Object> claims = new HashMap<>();
        if (role  != null) claims.put("role", role);
        if (email != null) claims.put("email", email);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86_400_000L))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private String buildExpiredToken(String username, String role) {
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis() - 100_000L))
                .setExpiration(new Date(System.currentTimeMillis() - 50_000L))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // ── valid token scenarios ──────────────────────────────────────────────────

    @Test
    void validToken_withEmailClaim_setsEmailAsDetails() throws Exception {
        String token = buildToken("alice", "USER", "alice@x.com");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertThat(auth.getName()).isEqualTo("alice");
        assertThat(auth.getDetails()).isEqualTo("alice@x.com");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void validToken_withoutEmailClaim_usesUsernameAsDetails() throws Exception {
        // email claim absent → fallback to username
        String token = buildToken("bob", "ADMIN", null);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertThat(auth.getName()).isEqualTo("bob");
        assertThat(auth.getDetails()).isEqualTo("bob");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void validToken_setsCorrectRole() throws Exception {
        String token = buildToken("carol", "ADMIN", "carol@x.com");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        boolean hasRole = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        assertTrue(hasRole);
    }

    @Test
    void validToken_userRole_setsUserAuthority() throws Exception {
        String token = buildToken("dave", "USER", "dave@x.com");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        boolean hasRole = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
        assertTrue(hasRole);
    }

    // ── missing / invalid headers ──────────────────────────────────────────────

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "Bearer invalid.token.here",
            "Basic dXNlcjpwYXNz",
            "Bearer ",
            "Token sometoken"
    })
    void invalidOrMissingHeader_doesNotSetAuthentication(String header) throws Exception {
        when(request.getHeader("Authorization")).thenReturn(header);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void expiredToken_doesNotSetAuthentication() throws Exception {
        String token = buildExpiredToken("expired", "USER");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void wrongSignatureToken_doesNotSetAuthentication() throws Exception {
        // Sign with a different key
        Key wrongKey = Keys.hmacShaKeyFor(
                "wrongwrongwrongwrongwrongwrongwrongwrongwrongwrongwrongwrongwrong12".getBytes());
        String token = Jwts.builder()
                .setSubject("hacker")
                .setExpiration(new Date(System.currentTimeMillis() + 86_400_000L))
                .signWith(wrongKey, SignatureAlgorithm.HS256)
                .compact();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void filterChain_alwaysContinuesEvenOnError() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer bad.token");

        filter.doFilterInternal(request, response, filterChain);

        // Filter chain must always proceed regardless of JWT errors
        verify(filterChain, times(1)).doFilter(request, response);
    }
}
