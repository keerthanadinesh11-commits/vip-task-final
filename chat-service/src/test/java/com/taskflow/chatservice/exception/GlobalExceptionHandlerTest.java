package com.taskflow.chatservice.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.chatservice.dto.SendMessageRequest;
import com.taskflow.chatservice.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for GlobalExceptionHandler via real HTTP calls through MockMvc.
 * Covers MethodArgumentNotValidException, IllegalArgumentException, and general Exception.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  ChatService chatService;

    // ── MethodArgumentNotValidException (validation errors) ────────────────────

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void validationError_missingTaskId_returns400WithFieldErrors() throws Exception {
        SendMessageRequest req = new SendMessageRequest();
        req.setContent("Hello");
        // taskId is null → @NotNull violation

        mockMvc.perform(post("/chat/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.taskId").exists());
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void validationError_missingContent_returns400WithFieldErrors() throws Exception {
        SendMessageRequest req = new SendMessageRequest();
        req.setTaskId(1L);
        // content is null → @NotBlank violation

        mockMvc.perform(post("/chat/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.content").exists());
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void validationError_bothFieldsMissing_returns400WithMultipleErrors() throws Exception {
        // completely empty request body → both fields invalid
        mockMvc.perform(post("/chat/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ── IllegalArgumentException ───────────────────────────────────────────────

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void illegalArgumentException_returns400WithMessage() throws Exception {
        when(chatService.sendMessage(any(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Invalid task ID"));

        SendMessageRequest req = new SendMessageRequest();
        req.setTaskId(1L);
        req.setContent("Hello");

        mockMvc.perform(post("/chat/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid task ID"))
                .andExpect(jsonPath("$.message").value("Invalid task ID"));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void illegalArgumentException_fromGetMessages_returns400() throws Exception {
        when(chatService.getMessages(anyLong(), anyString()))
                .thenThrow(new IllegalArgumentException("Task not found"));

        mockMvc.perform(get("/chat/messages/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Task not found"));
    }

    // ── General Exception (500) ────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void generalException_returns500WithGenericMessage() throws Exception {
        when(chatService.sendMessage(any(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Unexpected DB failure"));

        SendMessageRequest req = new SendMessageRequest();
        req.setTaskId(1L);
        req.setContent("Hello");

        mockMvc.perform(post("/chat/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void generalException_fromGetMessages_returns500() throws Exception {
        when(chatService.getMessages(anyLong(), anyString()))
                .thenThrow(new RuntimeException("DB connection lost"));

        mockMvc.perform(get("/chat/messages/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("An unexpected error occurred"));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void generalException_fromGetUnread_returns500() throws Exception {
        when(chatService.getUnreadCount(anyLong(), anyString()))
                .thenThrow(new RuntimeException("Unread count failed"));

        mockMvc.perform(get("/chat/unread/1"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void generalException_fromGetTotalUnread_returns500() throws Exception {
        when(chatService.getTotalUnreadCount(anyString()))
                .thenThrow(new RuntimeException("Total unread failed"));

        mockMvc.perform(get("/chat/unread/total"))
                .andExpect(status().isInternalServerError());
    }
}
