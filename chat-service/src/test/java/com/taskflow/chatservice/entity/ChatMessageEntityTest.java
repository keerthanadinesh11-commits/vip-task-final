package com.taskflow.chatservice.entity;

import com.taskflow.chatservice.dto.ChatMessageResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ChatMessage entity and ChatMessageResponse DTO.
 * Covers constructors, getters, setters, and the from() factory method.
 */
class ChatMessageEntityTest {

    // ── ChatMessage no-arg constructor ────────────────────────────────────────

    @Test
    void noArgConstructor_createsInstanceWithDefaultValues() {
        ChatMessage msg = new ChatMessage();

        assertThat(msg.getId()).isNull();
        assertThat(msg.getTaskId()).isNull();
        assertThat(msg.getSenderEmail()).isNull();
        assertThat(msg.getSenderName()).isNull();
        assertThat(msg.getSenderRole()).isNull();
        assertThat(msg.getContent()).isNull();
        assertThat(msg.isRead()).isFalse();
    }

    // ── ChatMessage all-args constructor ───────────────────────────────────────

    @Test
    void allArgsConstructor_setsAllFields() {
        ChatMessage msg = new ChatMessage(1L, "alice@x.com", "alice", "USER", "Hello!");

        assertThat(msg.getTaskId()).isEqualTo(1L);
        assertThat(msg.getSenderEmail()).isEqualTo("alice@x.com");
        assertThat(msg.getSenderName()).isEqualTo("alice");
        assertThat(msg.getSenderRole()).isEqualTo("USER");
        assertThat(msg.getContent()).isEqualTo("Hello!");
        assertThat(msg.isRead()).isFalse();
    }

    @Test
    void allArgsConstructor_isReadDefaultsFalse() {
        ChatMessage msg = new ChatMessage(5L, "bob@x.com", "bob", "ADMIN", "Test");

        assertThat(msg.isRead()).isFalse();
    }

    // ── Getters / Setters ──────────────────────────────────────────────────────

    @Test
    void settersAndGetters_workCorrectly() {
        ChatMessage msg = new ChatMessage();

        msg.setId(42L);
        msg.setTaskId(10L);
        msg.setSenderEmail("carol@x.com");
        msg.setSenderName("carol");
        msg.setSenderRole("MANAGER");
        msg.setContent("Updated content");
        LocalDateTime now = LocalDateTime.now();
        msg.setTimestamp(now);
        msg.setRead(true);

        assertThat(msg.getId()).isEqualTo(42L);
        assertThat(msg.getTaskId()).isEqualTo(10L);
        assertThat(msg.getSenderEmail()).isEqualTo("carol@x.com");
        assertThat(msg.getSenderName()).isEqualTo("carol");
        assertThat(msg.getSenderRole()).isEqualTo("MANAGER");
        assertThat(msg.getContent()).isEqualTo("Updated content");
        assertThat(msg.getTimestamp()).isEqualTo(now);
        assertThat(msg.isRead()).isTrue();
    }

    @Test
    void setRead_togglesBetweenTrueAndFalse() {
        ChatMessage msg = new ChatMessage(1L, "x@x.com", "x", "USER", "msg");

        assertThat(msg.isRead()).isFalse();
        msg.setRead(true);
        assertThat(msg.isRead()).isTrue();
        msg.setRead(false);
        assertThat(msg.isRead()).isFalse();
    }

    // ── ChatMessageResponse.from() ─────────────────────────────────────────────

    @Test
    void from_mapsAllFieldsCorrectly() {
        ChatMessage msg = new ChatMessage(3L, "dave@x.com", "dave", "USER", "Hi there");
        msg.setId(99L);
        LocalDateTime ts = LocalDateTime.of(2024, 1, 15, 10, 30);
        msg.setTimestamp(ts);
        msg.setRead(true);

        ChatMessageResponse resp = ChatMessageResponse.from(msg);

        assertThat(resp.getId()).isEqualTo(99L);
        assertThat(resp.getTaskId()).isEqualTo(3L);
        assertThat(resp.getSenderEmail()).isEqualTo("dave@x.com");
        assertThat(resp.getSenderName()).isEqualTo("dave");
        assertThat(resp.getSenderRole()).isEqualTo("USER");
        assertThat(resp.getContent()).isEqualTo("Hi there");
        assertThat(resp.getTimestamp()).isEqualTo(ts);
        assertThat(resp.isRead()).isTrue();
    }

    @Test
    void from_unreadMessage_isReadFalse() {
        ChatMessage msg = new ChatMessage(1L, "eve@x.com", "eve", "ADMIN", "New msg");
        msg.setId(5L);
        msg.setTimestamp(LocalDateTime.now());
        // isRead defaults to false

        ChatMessageResponse resp = ChatMessageResponse.from(msg);

        assertThat(resp.isRead()).isFalse();
    }

    @Test
    void from_nullTimestamp_mapsCorrectly() {
        ChatMessage msg = new ChatMessage(2L, "f@x.com", "f", "USER", "msg");
        msg.setId(7L);
        // timestamp not set (null)

        ChatMessageResponse resp = ChatMessageResponse.from(msg);

        assertThat(resp.getTimestamp()).isNull();
        assertThat(resp.getId()).isEqualTo(7L);
    }

    // ── SendMessageRequest ─────────────────────────────────────────────────────

    @Test
    void sendMessageRequest_gettersAndSetters_workCorrectly() {
        com.taskflow.chatservice.dto.SendMessageRequest req =
                new com.taskflow.chatservice.dto.SendMessageRequest();

        req.setTaskId(5L);
        req.setContent("Test message");

        assertThat(req.getTaskId()).isEqualTo(5L);
        assertThat(req.getContent()).isEqualTo("Test message");
    }

    @Test
    void sendMessageRequest_defaultConstructor_fieldsAreNull() {
        com.taskflow.chatservice.dto.SendMessageRequest req =
                new com.taskflow.chatservice.dto.SendMessageRequest();

        assertThat(req.getTaskId()).isNull();
        assertThat(req.getContent()).isNull();
    }

    // ── ChatMessageResponse no-arg constructor / getters ──────────────────────

    @Test
    void chatMessageResponse_noArgConstructor_createsInstance() {
        ChatMessageResponse resp = new ChatMessageResponse();

        assertThat(resp.getId()).isNull();
        assertThat(resp.getTaskId()).isNull();
        assertThat(resp.getSenderEmail()).isNull();
        assertThat(resp.getSenderName()).isNull();
        assertThat(resp.getSenderRole()).isNull();
        assertThat(resp.getContent()).isNull();
        assertThat(resp.getTimestamp()).isNull();
        assertThat(resp.isRead()).isFalse();
    }
}
