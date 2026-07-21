package com.taskflow.chatservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO for POST /chat/send request body.
 */
public class SendMessageRequest {

    @NotNull(message = "Task ID is required")
    private Long taskId;

    @NotBlank(message = "Message content is required")
    @Size(max = 1000, message = "Message cannot exceed 1000 characters")
    private String content;

    public SendMessageRequest() {}

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
