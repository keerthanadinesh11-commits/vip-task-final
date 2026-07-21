package com.taskflow.userservice.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class UserProfileEntityTest {

    @Test
    void defaultConstructor_createsEmptyProfile() {
        UserProfile p = new UserProfile();
        assertNull(p.getId());
        assertNull(p.getUsername());
        assertNull(p.getEmail());
        assertNull(p.getRole());
        assertNull(p.getBio());
        assertNull(p.getProfileImage());
        assertNull(p.getOtp());
        assertNull(p.getOtpExpiry());
        assertNull(p.getLastLogin());
        assertNull(p.getActivityLog());
    }

    @Test
    void paramConstructor_setsFields() {
        UserProfile p = new UserProfile(1L, "bob", "bob@x.com", "MANAGER");
        assertEquals(1L, p.getId());
        assertEquals("bob", p.getUsername());
        assertEquals("bob@x.com", p.getEmail());
        assertEquals("MANAGER", p.getRole());
    }

    @Test
    void settersAndGetters_workCorrectly() {
        UserProfile p = new UserProfile();
        LocalDateTime now = LocalDateTime.now();
        p.setId(5L);
        p.setUsername("carol");
        p.setEmail("carol@x.com");
        p.setRole("ADMIN");
        p.setBio("My bio");
        p.setProfileImage("photo.jpg");
        p.setOtp("112233");
        p.setOtpExpiry(now.plusMinutes(5));
        p.setLastLogin(now);
        p.setCreatedAt(now.minusDays(1));
        p.setActivityLog("[{\"action\":\"Login\"}]");

        assertEquals(5L, p.getId());
        assertEquals("carol", p.getUsername());
        assertEquals("carol@x.com", p.getEmail());
        assertEquals("ADMIN", p.getRole());
        assertEquals("My bio", p.getBio());
        assertEquals("photo.jpg", p.getProfileImage());
        assertEquals("112233", p.getOtp());
        assertEquals(now.plusMinutes(5), p.getOtpExpiry());
        assertEquals(now, p.getLastLogin());
        assertEquals(now.minusDays(1), p.getCreatedAt());
        assertEquals("[{\"action\":\"Login\"}]", p.getActivityLog());
    }
}
