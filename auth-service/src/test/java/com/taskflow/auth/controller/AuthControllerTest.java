package com.taskflow.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.auth.dto.AdminCreateUserRequest;
import com.taskflow.auth.dto.AuthRequest;
import com.taskflow.auth.dto.AuthResponse;
import com.taskflow.auth.dto.PasswordChangeRequest;
import com.taskflow.auth.dto.RegisterRequest;
import com.taskflow.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean AuthService authService;
    @MockBean AuthenticationManager authenticationManager;

    // ─── /auth/register ───────────────────────────────────────────────────────

    @Test
    void register_success_returns200() throws Exception {
        when(authService.register(any())).thenReturn("User registered successfully");

        RegisterRequest req = new RegisterRequest("alice", "alice@test.com", "Abcd1!");
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully"));
    }

    @Test
    void register_duplicateEmail_returns400() throws Exception {
        when(authService.register(any())).thenThrow(new IllegalArgumentException("Email already exists"));

        RegisterRequest req = new RegisterRequest("alice", "alice@test.com", "Abcd1!");
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email already exists"));
    }

    @Test
    void register_blankUsername_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest("", "alice@test.com", "Abcd1!");
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest("alice", "not-an-email", "Abcd1!");
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ─── /auth/login ──────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returnsToken() throws Exception {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("alice@test.com");
        doNothing().when(authService).validateLoginCredentials("alice@test.com", "Abcd1!");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(authService.generateTokenResponseByEmail("alice@test.com"))
                .thenReturn(new AuthResponse("jwt-token", "alice", "USER"));

        AuthRequest req = new AuthRequest("alice@test.com", "Abcd1!");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void login_badCredentials_returns401() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        AuthRequest req = new AuthRequest("bad@test.com", "WrongPass1!");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_incorrectEmail_returns401WithSpecificMessage() throws Exception {
        doThrow(new com.taskflow.auth.exception.InvalidCredentialsException("Incorrect email address"))
                .when(authService).validateLoginCredentials("nobody@test.com", "Abcd1!");

        AuthRequest req = new AuthRequest("nobody@test.com", "Abcd1!");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Incorrect email address"));
    }

    @Test
    void login_incorrectPassword_returns401WithSpecificMessage() throws Exception {
        doThrow(new com.taskflow.auth.exception.InvalidCredentialsException("Incorrect password"))
                .when(authService).validateLoginCredentials("alice@test.com", "WrongPass1!");

        AuthRequest req = new AuthRequest("alice@test.com", "WrongPass1!");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Incorrect password"));
    }

    @Test
    void login_blankEmail_returns400() throws Exception {
        AuthRequest req = new AuthRequest("", "Abcd1!");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ─── /auth/admin/users ────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCreateUser_success_returns201() throws Exception {
        when(authService.adminCreateUser(any()))
                .thenReturn(new AuthResponse(null, "bob", "USER"));

        AdminCreateUserRequest req = new AdminCreateUserRequest();
        req.setUsername("bob");
        req.setEmail("bob@test.com");
        req.setPassword("Abcd1!");
        req.setRole("USER");

        mockMvc.perform(post("/auth/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("bob"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void adminCreateUser_forbiddenForUser_returns403() throws Exception {
        AdminCreateUserRequest req = new AdminCreateUserRequest();
        req.setUsername("bob");
        req.setEmail("bob@test.com");
        req.setPassword("Abcd1!");

        mockMvc.perform(post("/auth/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ─── /auth/password ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "alice@test.com")
    void changePassword_success_returns200() throws Exception {
        doNothing().when(authService).changePassword(any(), any());

        PasswordChangeRequest req = new PasswordChangeRequest();
        req.setCurrentPassword("Old123!");
        req.setNewPassword("New456@");

        mockMvc.perform(put("/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password updated successfully"));
    }

    @Test
    @WithMockUser(username = "alice@test.com")
    void changePassword_wrongCurrent_returns400() throws Exception {
        doThrow(new IllegalArgumentException("Current password is incorrect"))
                .when(authService).changePassword(any(), any());

        PasswordChangeRequest req = new PasswordChangeRequest();
        req.setCurrentPassword("Wrong1!");
        req.setNewPassword("New456@");

        mockMvc.perform(put("/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_unauthenticated_returns401or403() throws Exception {
        PasswordChangeRequest req = new PasswordChangeRequest();
        req.setCurrentPassword("Old123!");
        req.setNewPassword("New456@");

        mockMvc.perform(put("/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(result ->
                        org.junit.jupiter.api.Assertions.assertTrue(
                                result.getResponse().getStatus() == 401 ||
                                result.getResponse().getStatus() == 403));
    }
}
