package com.taskflow.notificationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.notificationservice.dto.NotificationDto;
import com.taskflow.notificationservice.entity.Notification;
import com.taskflow.notificationservice.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean NotificationService notificationService;

    // ─── GET /notifications ───────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@x.com", roles = "ADMIN")
    void list_asAdmin_returnsAllNotifications() throws Exception {
        Notification n1 = new Notification(1L, "msg1", "alice@x.com", LocalDateTime.now());
        Notification n2 = new Notification(2L, "msg2", "bob@x.com", LocalDateTime.now());
        when(notificationService.getAll()).thenReturn(List.of(n1, n2));

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].message").value("msg1"));
    }

    @Test
    @WithMockUser(username = "alice@x.com", roles = "USER")
    void list_asUser_returnsOwnNotifications() throws Exception {
        Notification n = new Notification(1L, "msg1", "alice@x.com", LocalDateTime.now());
        when(notificationService.getForUser("alice@x.com")).thenReturn(List.of(n));

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value("alice@x.com"));
    }

    @Test
    @WithMockUser(username = "mgr@x.com", roles = "MANAGER")
    void list_asManager_returnsOwnNotifications() throws Exception {
        when(notificationService.getForUser("mgr@x.com")).thenReturn(List.of());

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void list_unauthenticated_returns401or403() throws Exception {
        mockMvc.perform(get("/notifications"))
                .andExpect(result ->
                        org.junit.jupiter.api.Assertions.assertTrue(
                                result.getResponse().getStatus() == 401 ||
                                result.getResponse().getStatus() == 403));
    }

    // ─── GET /notifications/{id} ──────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "USER")
    void getById_found_returns200() throws Exception {
        Notification n = new Notification(5L, "hello", "u@x.com", LocalDateTime.now());
        when(notificationService.findById(5L)).thenReturn(n);

        mockMvc.perform(get("/notifications/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.message").value("hello"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getById_notFound_returns404() throws Exception {
        when(notificationService.findById(99L))
                .thenThrow(new IllegalArgumentException("Notification not found with id: 99"));

        mockMvc.perform(get("/notifications/99"))
                .andExpect(status().isNotFound());
    }

    // ─── POST /notifications/internal ─────────────────────────────────────────

    @Test
    void createInternal_persistsAndReturns201() throws Exception {
        Notification saved = new Notification(10L, "task assigned", "user@x.com", LocalDateTime.now());
        when(notificationService.save(any())).thenReturn(saved);

        NotificationDto dto = new NotificationDto(null, "task assigned", "user@x.com", null);
        mockMvc.perform(post("/notifications/internal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.message").value("task assigned"))
                .andExpect(jsonPath("$.userId").value("user@x.com"));
    }
}
