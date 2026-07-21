package com.taskflow.auth.service;

import com.taskflow.auth.entity.UserCredential;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET =
            "taskflowsupersecretkeytaskflowsupersecretkeytaskflowsupersecretkey1234";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);
    }

    @Test
    void generateToken_returnsNonNullToken() {
        UserCredential user = new UserCredential(1L, "alice", "alice@x.com", "hashed", "USER");
        String token = jwtService.generateToken(user);
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void generateToken_containsThreeParts() {
        UserCredential user = new UserCredential(2L, "bob", "bob@x.com", "hashed", "ADMIN");
        String token = jwtService.generateToken(user);
        // JWT is header.payload.signature
        String[] parts = token.split("\\.");
        org.junit.jupiter.api.Assertions.assertEquals(3, parts.length);
    }

    @Test
    void generateToken_differentUsersProduceDifferentTokens() {
        UserCredential u1 = new UserCredential(1L, "alice", "alice@x.com", "h", "USER");
        UserCredential u2 = new UserCredential(2L, "bob", "bob@x.com", "h", "ADMIN");
        String t1 = jwtService.generateToken(u1);
        String t2 = jwtService.generateToken(u2);
        org.junit.jupiter.api.Assertions.assertNotEquals(t1, t2);
    }
}
