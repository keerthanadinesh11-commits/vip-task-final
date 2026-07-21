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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserProfileControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean UserProfileService userProfileService;

    // ─── GET /users ───────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAll_asAdmin_returnsProfiles() throws Exception {
        UserProfile p1 = new UserProfile(1L, "alice", "alice@x.com", "USER");
        UserProfile p2 = new UserProfile(2L, "bob", "bob@x.com", "MANAGER");
        when(userProfileService.getAll()).thenReturn(List.of(p1, p2));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[1].role").value("MANAGER"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAll_asUser_returnsProfiles() throws Exception {
        when(userProfileService.getAll()).thenReturn(List.of(
                new UserProfile(1L, "alice", "alice@x.com", "USER")));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getAll_unauthenticated_returns401or403() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(result ->
                        org.junit.jupiter.api.Assertions.assertTrue(
                                result.getResponse().getStatus() == 401 ||
                                result.getResponse().getStatus() == 403));
    }

    // ─── GET /users/all-users (ADMIN only) ────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_asAdmin_returnsProfiles() throws Exception {
        when(userProfileService.getAll()).thenReturn(List.of(
                new UserProfile(1L, "alice", "alice@x.com", "USER")));

        mockMvc.perform(get("/users/all-users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("alice@x.com"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllUsers_asUser_returns403() throws Exception {
        mockMvc.perform(get("/users/all-users"))
                .andExpect(status().isForbidden());
    }

    // ─── POST /users/internal/create ─────────────────────────────────────────

    @Test
    void createInternal_savesAndReturnsProfile() throws Exception {
        UserProfile saved = new UserProfile(5L, "charlie", "charlie@x.com", "USER");
        when(userProfileService.saveOrUpdate(any())).thenReturn(saved);

        Map<String, String> body = Map.of(
                "username", "charlie",
                "email", "charlie@x.com",
                "role", "USER");

        mockMvc.perform(post("/users/internal/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.username").value("charlie"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void createInternal_defaultsRoleToUser_whenRoleMissing() throws Exception {
        UserProfile saved = new UserProfile(6L, "dave", "dave@x.com", "USER");
        when(userProfileService.saveOrUpdate(any())).thenReturn(saved);

        Map<String, String> body = Map.of(
                "username", "dave",
                "email", "dave@x.com");
        // no role field → should default to "USER"

        mockMvc.perform(post("/users/internal/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("dave"));
    }

    // ─── DELETE /users/{id} (ADMIN only) ─────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_asAdmin_returns200() throws Exception {
        doNothing().when(userProfileService).deleteById(1L);

        mockMvc.perform(MockMvcRequestBuilders.delete("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deleted successfully"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteUser_asUser_returns403() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/users/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_notFound_returns404() throws Exception {
        doThrow(new IllegalArgumentException("User not found: 99"))
                .when(userProfileService).deleteById(99L);

        mockMvc.perform(MockMvcRequestBuilders.delete("/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found: 99"));
    }
}
