package com.taskflow.notificationservice.controller;

import com.taskflow.notificationservice.dto.NotificationDto;
import com.taskflow.notificationservice.entity.Notification;
import com.taskflow.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal endpoint used by task-service over Feign. Not exposed externally
 * — the gateway routes nothing to {@code /notifications/internal/**}.
 */
@RestController
@RequestMapping("/notifications/internal")
@Tag(name = "Notifications (internal)",
        description = "Inter-service endpoint used by task-service to record notifications")
public class NotificationInternalController {

    private final NotificationService service;

    public NotificationInternalController(NotificationService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Internal: store a notification")
    public ResponseEntity<NotificationDto> create(@RequestBody NotificationDto dto) {
        Notification entity = new Notification();
        entity.setMessage(dto.getMessage());
        entity.setUserId(dto.getUserId());
        Notification saved = service.save(entity);
        NotificationDto body = new NotificationDto(saved.getId(), saved.getMessage(),
                saved.getUserId(), saved.getTimestamp());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}
