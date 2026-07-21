package com.taskflow.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.userservice.entity.UserProfile;
import com.taskflow.userservice.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserProfileControllerNewEndpointsTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean UserProfileService userProfileService;

    // ── GET /users/profile ────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "alice@x.com", roles = "USER")
    void getProfile_returnsCurrentUserProfile() throws Exception {
        UserProfile p = new UserProfile(1L, "alice", "alice@x.com", "USER");
        p.setBio("Hello world");
        p.setLastLogin(LocalDateTime.now().minusHours(1));
        p.setCreatedAt(LocalDateTime.now().minusDays(10));
        when(userProfileService.getProfile("alice@x.com")).thenReturn(p);

        mockMvc.perform(get("/users/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@x.com"))
                .andExpect(jsonPath("$.bio").value("Hello world"));
    }

    @Test
    @WithMockUser(username = "admin@x.com", roles = "ADMIN")
    void getProfile_asAdmin_returnsProfile() throws Exception {
        UserProfile p = new UserProfile(2L, "admin", "admin@x.com", "ADMIN");
        when(userProfileService.getProfile("admin@x.com")).thenReturn(p);

        mockMvc.perform(get("/users/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void getProfile_unauthenticated_returns401or403() throws Exception {
        mockMvc.perform(get("/users/profile"))
                .andExpect(result ->
                        org.junit.jupiter.api.Assertions.assertTrue(
                                result.getResponse().getStatus() == 401 ||
                                result.getResponse().getStatus() == 403));
    }

    // ── PUT /users/profile ────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "alice@x.com", roles = "USER")
    void updateProfile_updatesAndReturns() throws Exception {
        UserProfile updated = new UserProfile(1L, "alice2", "alice@x.com", "USER");
        updated.setBio("Updated bio");
        when(userProfileService.updateProfile(eq("alice@x.com"), eq("alice2"), eq("Updated bio")))
                .thenReturn(updated);

        Map<String, String> body = Map.of("username", "alice2", "bio", "Updated bio");

        mockMvc.perform(put("/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice2"))
                .andExpect(jsonPath("$.bio").value("Updated bio"));
    }

    // ── POST /users/send-otp ──────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "alice@x.com", roles = "USER")
    void sendOtp_returns200WithMessage() throws Exception {
        doNothing().when(userProfileService).sendOtp("alice@x.com");

        mockMvc.perform(post("/users/send-otp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP sent to your registered email"));
    }

    @Test
    void sendOtp_unauthenticated_returns401or403() throws Exception {
        mockMvc.perform(post("/users/send-otp"))
                .andExpect(result ->
                        org.junit.jupiter.api.Assertions.assertTrue(
                                result.getResponse().getStatus() == 401 ||
                                result.getResponse().getStatus() == 403));
    }

    // ── POST /users/verify-otp ────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "alice@x.com", roles = "USER")
    void verifyOtp_returnsOk_whenValid() throws Exception {
        when(userProfileService.verifyOtp("alice@x.com", "123456")).thenReturn(true);

        Map<String, String> body = Map.of("otp", "123456");

        mockMvc.perform(post("/users/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP verified successfully"));
    }

    @Test
    @WithMockUser(username = "alice@x.com", roles = "USER")
    void verifyOtp_returns400_whenInvalid() throws Exception {
        when(userProfileService.verifyOtp("alice@x.com", "000000")).thenReturn(false);

        Map<String, String> body = Map.of("otp", "000000");

        mockMvc.perform(post("/users/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired OTP"));
    }

    // ── POST /users/change-password ───────────────────────────────────────────

    @Test
    @WithMockUser(username = "alice@x.com", roles = "USER")
    void changePassword_returnsOk_onSuccess() throws Exception {
        doNothing().when(userProfileService).changePassword("alice@x.com", "123456", "newPass1!");

        Map<String, String> body = Map.of("otp", "123456", "newPassword", "newPass1!");

        mockMvc.perform(post("/users/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
    }

    @Test
    @WithMockUser(username = "alice@x.com", roles = "USER")
    void changePassword_returns400_whenOtpInvalid() throws Exception {
        doThrow(new IllegalArgumentException("Invalid or expired OTP"))
                .when(userProfileService).changePassword("alice@x.com", "wrong", "newPass1!");

        Map<String, String> body = Map.of("otp", "wrong", "newPassword", "newPass1!");

        mockMvc.perform(post("/users/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired OTP"));
    }

    // ── POST /users/upload-photo ──────────────────────────────────────────────

    @Test
    @WithMockUser(username = "alice@x.com", roles = "USER")
    void uploadPhoto_returnsFilename() throws Exception {
        when(userProfileService.uploadPhoto(eq("alice@x.com"), any())).thenReturn("abc123.jpg");

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "fake-image-content".getBytes());

        mockMvc.perform(multipart("/users/upload-photo").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("abc123.jpg"))
                .andExpect(jsonPath("$.message").value("Photo uploaded successfully"));
    }

    // ── DELETE /users/delete-account ─────────────────────────────────────────

    @Test
    @WithMockUser(username = "alice@x.com", roles = "USER")
    void deleteAccount_returnsOk_whenOtpValid() throws Exception {
        doNothing().when(userProfileService).deleteAccount("alice@x.com", "123456");

        Map<String, String> body = Map.of("otp", "123456");

        mockMvc.perform(delete("/users/delete-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account deleted successfully"));
    }

    @Test
    @WithMockUser(username = "alice@x.com", roles = "USER")
    void deleteAccount_returns400_whenOtpInvalid() throws Exception {
        doThrow(new IllegalArgumentException("Invalid or expired OTP"))
                .when(userProfileService).deleteAccount("alice@x.com", "000000");

        Map<String, String> body = Map.of("otp", "000000");

        mockMvc.perform(delete("/users/delete-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ── POST /users/internal/record-login ────────────────────────────────────

    @Test
    void recordLogin_internal_returns200() throws Exception {
        doNothing().when(userProfileService).recordLogin("alice@x.com");

        Map<String, String> body = Map.of("email", "alice@x.com");

        mockMvc.perform(post("/users/internal/record-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    // ── GET /users/profile-photo/{filename} ──────────────────────────────────

    @Test
    void getProfilePhoto_returns404_whenFileNotFound() throws Exception {
        mockMvc.perform(get("/users/profile-photo/nonexistent.jpg"))
                .andExpect(status().isNotFound());
    }
}
