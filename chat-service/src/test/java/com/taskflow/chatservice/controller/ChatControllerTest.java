package com.taskflow.chatservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.chatservice.dto.ChatMessageResponse;
import com.taskflow.chatservice.dto.SendMessageRequest;
import com.taskflow.chatservice.entity.ChatMessage;
import com.taskflow.chatservice.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  ChatService chatService;

    private ChatMessageResponse sampleResponse;

    @BeforeEach
    void setUp() {
        // Build via the from() factory using a real ChatMessage entity
        ChatMessage entity = new ChatMessage(10L, "alice@x.com", "alice", "USER", "Hello!");
        entity.setId(1L);
        entity.setTimestamp(LocalDateTime.now());
        sampleResponse = ChatMessageResponse.from(entity);
    }

    // ── POST /chat/send ────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void sendMessage_validRequest_returns200() throws Exception {
        when(chatService.sendMessage(any(), anyString(), anyString(), anyString()))
                .thenReturn(sampleResponse);

        SendMessageRequest req = new SendMessageRequest();
        req.setTaskId(10L);
        req.setContent("Hello!");

        mockMvc.perform(post("/chat/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(10))
                .andExpect(jsonPath("$.content").value("Hello!"))
                .andExpect(jsonPath("$.senderName").value("alice"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void sendMessage_asAdmin_returns200() throws Exception {
        ChatMessage adminEntity = new ChatMessage(5L, "admin@x.com", "admin", "ADMIN", "Admin reply");
        adminEntity.setId(2L);
        adminEntity.setTimestamp(LocalDateTime.now());
        ChatMessageResponse adminResp = ChatMessageResponse.from(adminEntity);

        when(chatService.sendMessage(any(), anyString(), anyString(), anyString()))
                .thenReturn(adminResp);

        SendMessageRequest req = new SendMessageRequest();
        req.setTaskId(5L);
        req.setContent("Admin reply");

        mockMvc.perform(post("/chat/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderRole").value("ADMIN"));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void sendMessage_missingTaskId_returns400() throws Exception {
        SendMessageRequest req = new SendMessageRequest();
        req.setContent("Hello without taskId");
        // taskId is null → @NotNull violation

        mockMvc.perform(post("/chat/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void sendMessage_blankContent_returns400() throws Exception {
        SendMessageRequest req = new SendMessageRequest();
        req.setTaskId(1L);
        req.setContent("   ");  // blank → @NotBlank violation

        mockMvc.perform(post("/chat/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void sendMessage_emptyContent_returns400() throws Exception {
        SendMessageRequest req = new SendMessageRequest();
        req.setTaskId(1L);
        req.setContent("");

        mockMvc.perform(post("/chat/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendMessage_unauthenticated_returns401or403() throws Exception {
        SendMessageRequest req = new SendMessageRequest();
        req.setTaskId(1L);
        req.setContent("Hello");

        mockMvc.perform(post("/chat/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(result ->
                    org.junit.jupiter.api.Assertions.assertTrue(
                        result.getResponse().getStatus() == 401 ||
                        result.getResponse().getStatus() == 403));
    }

    // ── GET /chat/messages/{taskId} ────────────────────────────────────────────

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void getMessages_returnsListOfMessages() throws Exception {
        ChatMessage entity2 = new ChatMessage(10L, "bob@x.com", "bob", "ADMIN", "Reply");
        entity2.setId(2L);
        entity2.setTimestamp(LocalDateTime.now());
        entity2.setRead(true);
        ChatMessageResponse r2 = ChatMessageResponse.from(entity2);

        when(chatService.getMessages(eq(10L), anyString()))
                .thenReturn(List.of(sampleResponse, r2));

        mockMvc.perform(get("/chat/messages/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("Hello!"))
                .andExpect(jsonPath("$[1].content").value("Reply"));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void getMessages_emptyTask_returnsEmptyList() throws Exception {
        when(chatService.getMessages(eq(99L), anyString())).thenReturn(List.of());

        mockMvc.perform(get("/chat/messages/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void getMessages_singleMessage_returnsSingleItem() throws Exception {
        when(chatService.getMessages(eq(1L), anyString())).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/chat/messages/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].senderEmail").value("alice@x.com"));
    }

    @Test
    void getMessages_unauthenticated_returns401or403() throws Exception {
        mockMvc.perform(get("/chat/messages/1"))
                .andExpect(result ->
                    org.junit.jupiter.api.Assertions.assertTrue(
                        result.getResponse().getStatus() == 401 ||
                        result.getResponse().getStatus() == 403));
    }

    // ── GET /chat/unread/{taskId} ──────────────────────────────────────────────

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void getUnreadCount_returnsCount() throws Exception {
        when(chatService.getUnreadCount(eq(10L), anyString())).thenReturn(3L);

        mockMvc.perform(get("/chat/unread/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(3));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void getUnreadCount_zeroUnread_returnsZero() throws Exception {
        when(chatService.getUnreadCount(eq(5L), anyString())).thenReturn(0L);

        mockMvc.perform(get("/chat/unread/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    @Test
    void getUnreadCount_unauthenticated_returns401or403() throws Exception {
        mockMvc.perform(get("/chat/unread/1"))
                .andExpect(result ->
                    org.junit.jupiter.api.Assertions.assertTrue(
                        result.getResponse().getStatus() == 401 ||
                        result.getResponse().getStatus() == 403));
    }

    // ── GET /chat/unread/total ─────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void getTotalUnread_returnsCount() throws Exception {
        when(chatService.getTotalUnreadCount(anyString())).thenReturn(7L);

        mockMvc.perform(get("/chat/unread/total"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUnread").value(7));
    }

    @Test
    @WithMockUser(username = "bob", roles = "ADMIN")
    void getTotalUnread_asAdmin_returnsCount() throws Exception {
        when(chatService.getTotalUnreadCount(anyString())).thenReturn(12L);

        mockMvc.perform(get("/chat/unread/total"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUnread").value(12));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void getTotalUnread_zero_returnsZero() throws Exception {
        when(chatService.getTotalUnreadCount(anyString())).thenReturn(0L);

        mockMvc.perform(get("/chat/unread/total"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUnread").value(0));
    }

    @Test
    void getTotalUnread_unauthenticated_returns401or403() throws Exception {
        mockMvc.perform(get("/chat/unread/total"))
                .andExpect(result ->
                    org.junit.jupiter.api.Assertions.assertTrue(
                        result.getResponse().getStatus() == 401 ||
                        result.getResponse().getStatus() == 403));
    }
}
