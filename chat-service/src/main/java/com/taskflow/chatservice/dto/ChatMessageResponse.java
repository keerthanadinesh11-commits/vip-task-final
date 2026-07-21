package com.taskflow.chatservice.dto;

import com.taskflow.chatservice.entity.ChatMessage;

import java.time.LocalDateTime;

/**
 * DTO returned to the frontend for each chat message.
 * Converts entity to a clean JSON response.
 */
public class ChatMessageResponse {

    private Long id;
    private Long taskId;
    private String senderEmail;
    private String senderName;
    private String senderRole;
    private String content;
    private LocalDateTime timestamp;
    private boolean isRead;

    public ChatMessageResponse() {}

    /**
     * Converts a ChatMessage entity to response DTO.
     */
    public static ChatMessageResponse from(ChatMessage msg) {
        ChatMessageResponse res = new ChatMessageResponse();
        res.id = msg.getId();
        res.taskId = msg.getTaskId();
        res.senderEmail = msg.getSenderEmail();
        res.senderName = msg.getSenderName();
        res.senderRole = msg.getSenderRole();
        res.content = msg.getContent();
        res.timestamp = msg.getTimestamp();
        res.isRead = msg.isRead();
        return res;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Long getId() { return id; }
    public Long getTaskId() { return taskId; }
    public String getSenderEmail() { return senderEmail; }
    public String getSenderName() { return senderName; }
    public String getSenderRole() { return senderRole; }
    public String getContent() { return content; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isRead() { return isRead; }
}
