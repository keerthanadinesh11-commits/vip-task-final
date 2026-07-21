package com.taskflow.userservice.dto;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class DtoTest {

    @Test
    void userProfileDto_defaultConstructor() {
        UserProfileDto dto = new UserProfileDto();
        assertNull(dto.getId());
        assertNull(dto.getUsername());
    }

    @Test
    void userProfileDto_paramConstructor() {
        UserProfileDto dto = new UserProfileDto(1L, "alice", "alice@x.com", "USER");
        assertEquals(1L, dto.getId());
        assertEquals("alice", dto.getUsername());
        assertEquals("alice@x.com", dto.getEmail());
        assertEquals("USER", dto.getRole());
    }

    @Test
    void userProfileDto_settersAndGetters() {
        UserProfileDto dto = new UserProfileDto();
        LocalDateTime now = LocalDateTime.now();
        dto.setId(2L);
        dto.setUsername("bob");
        dto.setEmail("bob@x.com");
        dto.setRole("MANAGER");
        dto.setBio("Bio text");
        dto.setProfileImage("img.jpg");
        dto.setLastLogin(now);
        dto.setCreatedAt(now.minusDays(1));
        dto.setActivityLog("[]");

        assertEquals(2L, dto.getId());
        assertEquals("bob", dto.getUsername());
        assertEquals("MANAGER", dto.getRole());
        assertEquals("Bio text", dto.getBio());
        assertEquals("img.jpg", dto.getProfileImage());
        assertEquals(now, dto.getLastLogin());
        assertEquals("[]", dto.getActivityLog());
    }

    @Test
    void updateProfileRequest_settersAndGetters() {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setUsername("newUser");
        req.setBio("new bio");
        assertEquals("newUser", req.getUsername());
        assertEquals("new bio", req.getBio());
    }

    @Test
    void changePasswordRequest_settersAndGetters() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("old");
        req.setOtp("123456");
        req.setNewPassword("new");
        assertEquals("old", req.getOldPassword());
        assertEquals("123456", req.getOtp());
        assertEquals("new", req.getNewPassword());
    }

    @Test
    void verifyOtpRequest_settersAndGetters() {
        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setOtp("654321");
        assertEquals("654321", req.getOtp());
    }
}
