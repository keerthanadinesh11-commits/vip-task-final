package com.taskflow.chatservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a single chat message between a user and admin for a specific task.
 */
@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long taskId;

    @Column(nullable = false, length = 100)
    private String senderEmail;

    @Column(nullable = false, length = 50)
    private String senderName;

    @Column(nullable = false, length = 20)
    private String senderRole;

    @Column(nullable = false, length = 1000)
    private String content;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private boolean isRead;

    public ChatMessage() {}

    public ChatMessage(Long taskId, String senderEmail, String senderName,
                       String senderRole, String content) {
        this.taskId = taskId;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.senderRole = senderRole;
        this.content = content;
        this.isRead = false;
    }

    public Long getId()                         { return id; }
    public void setId(Long id)                  { this.id = id; }

    public Long getTaskId()                     { return taskId; }
    public void setTaskId(Long taskId)          { this.taskId = taskId; }

    public String getSenderEmail()              { return senderEmail; }
    public void setSenderEmail(String e)        { this.senderEmail = e; }

    public String getSenderName()               { return senderName; }
    public void setSenderName(String n)         { this.senderName = n; }

    public String getSenderRole()               { return senderRole; }
    public void setSenderRole(String r)         { this.senderRole = r; }

    public String getContent()                  { return content; }
    public void setContent(String content)      { this.content = content; }

    public LocalDateTime getTimestamp()         { return timestamp; }
    public void setTimestamp(LocalDateTime t)   { this.timestamp = t; }

    public boolean isRead()                     { return isRead; }
    public void setRead(boolean read)           { this.isRead = read; }
}
