package com.taskflow.notificationservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO that represents the Kafka message published by task-service.
 * Field names must match those in task-service's NotificationDto.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationEventDto {

    private String message;
    private String userId;

    public NotificationEventDto() {}

    public NotificationEventDto(String message, String userId) {
        this.message = message;
        this.userId = userId;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    @Override
    public String toString() {
        return "NotificationEventDto{userId='" + userId + "', message='" + message + "'}";
    }
}
