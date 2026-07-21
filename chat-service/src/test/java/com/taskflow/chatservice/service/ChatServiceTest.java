package com.taskflow.chatservice.service;

import com.taskflow.chatservice.dto.ChatMessageResponse;
import com.taskflow.chatservice.dto.SendMessageRequest;
import com.taskflow.chatservice.entity.ChatMessage;
import com.taskflow.chatservice.repository.ChatMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatRepository;

    @InjectMocks
    private ChatService chatService;

    private ChatMessage sampleMessage;

    @BeforeEach
    void setUp() {
        sampleMessage = new ChatMessage(1L, "alice@x.com", "alice", "USER", "Hello!");
        sampleMessage.setId(10L);
        sampleMessage.setTimestamp(LocalDateTime.now());
        sampleMessage.setRead(false);
    }

    // ── sendMessage ────────────────────────────────────────────────────────────

    @Test
    void sendMessage_savesAndReturnsResponse() {
        SendMessageRequest req = new SendMessageRequest();
        req.setTaskId(1L);
        req.setContent("  Hello!  ");

        when(chatRepository.save(any(ChatMessage.class))).thenReturn(sampleMessage);

        ChatMessageResponse resp = chatService.sendMessage(req, "alice@x.com", "alice", "USER");

        assertThat(resp).isNotNull();
        assertThat(resp.getId()).isEqualTo(10L);
        assertThat(resp.getTaskId()).isEqualTo(1L);
        assertThat(resp.getSenderEmail()).isEqualTo("alice@x.com");
        assertThat(resp.getSenderName()).isEqualTo("alice");
        assertThat(resp.getSenderRole()).isEqualTo("USER");
        assertThat(resp.getContent()).isEqualTo("Hello!");

        verify(chatRepository, times(1)).save(any(ChatMessage.class));
    }

    @Test
    void sendMessage_trimsWhitespaceFromContent() {
        SendMessageRequest req = new SendMessageRequest();
        req.setTaskId(2L);
        req.setContent("   spaces   ");

        ChatMessage saved = new ChatMessage(2L, "bob@x.com", "bob", "ADMIN", "spaces");
        saved.setId(11L);
        saved.setTimestamp(LocalDateTime.now());

        when(chatRepository.save(argThat(m -> m.getContent().equals("spaces"))))
                .thenReturn(saved);

        ChatMessageResponse resp = chatService.sendMessage(req, "bob@x.com", "bob", "ADMIN");

        assertThat(resp.getContent()).isEqualTo("spaces");
    }

    @Test
    void sendMessage_withAdminRole_savesCorrectly() {
        SendMessageRequest req = new SendMessageRequest();
        req.setTaskId(5L);
        req.setContent("Admin note");

        ChatMessage saved = new ChatMessage(5L, "admin@x.com", "admin", "ADMIN", "Admin note");
        saved.setId(20L);
        saved.setTimestamp(LocalDateTime.now());

        when(chatRepository.save(any(ChatMessage.class))).thenReturn(saved);

        ChatMessageResponse resp = chatService.sendMessage(req, "admin@x.com", "admin", "ADMIN");

        assertThat(resp.getSenderRole()).isEqualTo("ADMIN");
        assertThat(resp.getTaskId()).isEqualTo(5L);
    }

    // ── getMessages ────────────────────────────────────────────────────────────

    @Test
    void getMessages_marksAsReadAndReturnsMessages() {
        ChatMessage msg1 = new ChatMessage(1L, "bob@x.com", "bob", "ADMIN", "Hi");
        msg1.setId(1L);
        msg1.setTimestamp(LocalDateTime.now().minusMinutes(2));

        ChatMessage msg2 = new ChatMessage(1L, "alice@x.com", "alice", "USER", "Hey");
        msg2.setId(2L);
        msg2.setTimestamp(LocalDateTime.now());

        when(chatRepository.findByTaskIdOrderByTimestampAsc(1L)).thenReturn(List.of(msg1, msg2));

        List<ChatMessageResponse> result = chatService.getMessages(1L, "alice@x.com");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSenderEmail()).isEqualTo("bob@x.com");
        assertThat(result.get(1).getSenderEmail()).isEqualTo("alice@x.com");

        verify(chatRepository).markAllAsReadForTask(1L, "alice@x.com");
        verify(chatRepository).findByTaskIdOrderByTimestampAsc(1L);
    }

    @Test
    void getMessages_emptyTask_returnsEmptyList() {
        when(chatRepository.findByTaskIdOrderByTimestampAsc(99L)).thenReturn(List.of());

        List<ChatMessageResponse> result = chatService.getMessages(99L, "alice@x.com");

        assertThat(result).isEmpty();
        verify(chatRepository).markAllAsReadForTask(99L, "alice@x.com");
    }

    @Test
    void getMessages_singleMessage_returnsSingleItem() {
        when(chatRepository.findByTaskIdOrderByTimestampAsc(3L))
                .thenReturn(List.of(sampleMessage));

        List<ChatMessageResponse> result = chatService.getMessages(3L, "bob@x.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(10L);
    }

    // ── getUnreadCount ─────────────────────────────────────────────────────────

    @Test
    void getUnreadCount_returnsRepositoryValue() {
        when(chatRepository.countUnreadByTaskIdAndNotSender(1L, "alice@x.com")).thenReturn(3L);

        long count = chatService.getUnreadCount(1L, "alice@x.com");

        assertThat(count).isEqualTo(3L);
        verify(chatRepository).countUnreadByTaskIdAndNotSender(1L, "alice@x.com");
    }

    @Test
    void getUnreadCount_noUnread_returnsZero() {
        when(chatRepository.countUnreadByTaskIdAndNotSender(2L, "bob@x.com")).thenReturn(0L);

        long count = chatService.getUnreadCount(2L, "bob@x.com");

        assertThat(count).isEqualTo(0L);
    }

    @Test
    void getUnreadCount_largeCount_returnsCorrectValue() {
        when(chatRepository.countUnreadByTaskIdAndNotSender(10L, "user@x.com")).thenReturn(100L);

        long count = chatService.getUnreadCount(10L, "user@x.com");

        assertThat(count).isEqualTo(100L);
    }

    // ── getTotalUnreadCount ────────────────────────────────────────────────────

    @Test
    void getTotalUnreadCount_returnsRepositoryValue() {
        when(chatRepository.countAllUnreadForUser("alice@x.com")).thenReturn(7L);

        long total = chatService.getTotalUnreadCount("alice@x.com");

        assertThat(total).isEqualTo(7L);
        verify(chatRepository).countAllUnreadForUser("alice@x.com");
    }

    @Test
    void getTotalUnreadCount_noUnread_returnsZero() {
        when(chatRepository.countAllUnreadForUser("clean@x.com")).thenReturn(0L);

        long total = chatService.getTotalUnreadCount("clean@x.com");

        assertThat(total).isEqualTo(0L);
    }

    @Test
    void getTotalUnreadCount_callsCorrectMethod() {
        when(chatRepository.countAllUnreadForUser(anyString())).thenReturn(5L);

        chatService.getTotalUnreadCount("someone@x.com");

        verify(chatRepository, times(1)).countAllUnreadForUser("someone@x.com");
        verify(chatRepository, never()).countUnreadByTaskIdAndNotSender(anyLong(), anyString());
    }
}
