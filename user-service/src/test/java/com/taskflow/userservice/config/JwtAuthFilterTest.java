package com.taskflow.userservice.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

    private String buildToken(String email, String role) {
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000L))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // ✅ VALID TOKEN TEST
    @Test
    void validToken_populatesSecurityContext() throws Exception {
        String token = buildToken("alice@x.com", "USER");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    // ✅ PARAMETERIZED TEST (replaces 3 tests)
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "Bearer not.valid.token",
            "Basic abc123"
    })
    void invalidOrMissingHeader_doesNotPopulateContext(String header) throws Exception {

        when(request.getHeader("Authorization")).thenReturn(header);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    // ✅ ADMIN ROLE TEST (unchanged)
    @Test
    void adminToken_setsAdminRole() throws Exception {
        String token = buildToken("admin@x.com", "ADMIN");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());

        boolean hasAdmin = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        assertTrue(hasAdmin);
    }
}