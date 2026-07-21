package com.taskflow.notificationservice.controller;

import com.taskflow.notificationservice.dto.NotificationDto;
import com.taskflow.notificationservice.entity.Notification;
import com.taskflow.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "Read notifications")
@SecurityRequirement(name = "Bearer Authentication")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    /**
     * Returns notifications visible to the caller. Admins see everything;
     * other users only see their own notifications.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','USER')")
    @Operation(summary = "List notifications scoped by role")
    public ResponseEntity<List<NotificationDto>> list(Authentication authentication) {
        String email = authentication.getName();
        boolean admin = isAdmin(authentication);
        List<Notification> rows = admin ? service.getAll() : service.getForUser(email);
        return ResponseEntity.ok(rows.stream().map(NotificationController::toDto).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','USER')")
    @Operation(summary = "Fetch a single notification by id")
    public ResponseEntity<NotificationDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toDto(service.findById(id)));
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a) || "ROLE_ADMIN".equals(a));
    }

    private static NotificationDto toDto(Notification entity) {
        return new NotificationDto(entity.getId(),
                entity.getMessage(),
                entity.getUserId(),
                entity.getTimestamp());
    }
}
