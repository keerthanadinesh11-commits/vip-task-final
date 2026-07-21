package com.taskflow.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.auth.dto.ForgotPasswordRequest;
import com.taskflow.auth.dto.ResetPasswordRequest;
import com.taskflow.auth.dto.SendOtpRequest;
import com.taskflow.auth.dto.VerifyOtpRequest;
import com.taskflow.auth.service.ForgotPasswordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ForgotPasswordControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  ForgotPasswordService forgotPasswordService;

    // ── POST /auth/send-otp ───────────────────────────────────────────────────

    @Test
    void sendOtp_success_returns200() throws Exception {
        doNothing().when(forgotPasswordService).generateOtp(anyString());
        SendOtpRequest req = new SendOtpRequest();
        req.setEmail("user@test.com");
        mockMvc.perform(post("/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void sendOtp_unknownEmail_returns400() throws Exception {
        doThrow(new IllegalArgumentException("No account found"))
                .when(forgotPasswordService).generateOtp(anyString());
        SendOtpRequest req = new SendOtpRequest();
        req.setEmail("nobody@test.com");
        mockMvc.perform(post("/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendOtp_invalidEmail_returns400() throws Exception {
        SendOtpRequest req = new SendOtpRequest();
        req.setEmail("not-an-email");
        mockMvc.perform(post("/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── POST /auth/forgot-password (legacy) ───────────────────────────────────

    @Test
    void forgotPassword_success_returns200() throws Exception {
        doNothing().when(forgotPasswordService).generateOtp(anyString());
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("user@test.com");
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void forgotPassword_unknownEmail_returns400() throws Exception {
        doThrow(new IllegalArgumentException("No account found"))
                .when(forgotPasswordService).generateOtp(anyString());
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("nobody@test.com");
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void forgotPassword_invalidEmail_returns400() throws Exception {
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("not-an-email");
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── POST /auth/verify-otp ─────────────────────────────────────────────────

    @Test
    void verifyOtp_success_returns200() throws Exception {
        doNothing().when(forgotPasswordService).verifyOtp(anyString(), anyString());
        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail("user@test.com");
        req.setOtp("123456");
        mockMvc.perform(post("/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP verified successfully"));
    }

    @Test
    void verifyOtp_invalidOtp_returns400() throws Exception {
        doThrow(new IllegalArgumentException("Invalid OTP"))
                .when(forgotPasswordService).verifyOtp(anyString(), anyString());
        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail("user@test.com");
        req.setOtp("000000");
        mockMvc.perform(post("/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyOtp_expiredOtp_returns400() throws Exception {
        doThrow(new IllegalArgumentException("OTP has expired"))
                .when(forgotPasswordService).verifyOtp(anyString(), anyString());
        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail("user@test.com");
        req.setOtp("123456");
        mockMvc.perform(post("/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyOtp_wrongLength_returns400() throws Exception {
        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail("user@test.com");
        req.setOtp("12");
        mockMvc.perform(post("/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── POST /auth/reset-password ─────────────────────────────────────────────

    @Test
    void resetPassword_success_returns200() throws Exception {
        doNothing().when(forgotPasswordService).resetPassword(anyString(), anyString(), anyString());
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail("user@test.com");
        req.setOtp("123456");
        req.setNewPassword("NewPass1!");
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully"));
    }

    @Test
    void resetPassword_otpNotVerified_returns400() throws Exception {
        doThrow(new IllegalArgumentException("OTP not verified"))
                .when(forgotPasswordService).resetPassword(anyString(), anyString(), anyString());
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail("user@test.com");
        req.setOtp("999999");
        req.setNewPassword("NewPass1!");
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPassword_weakPassword_returns400() throws Exception {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail("user@test.com");
        req.setOtp("123456");
        req.setNewPassword("weak");
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
