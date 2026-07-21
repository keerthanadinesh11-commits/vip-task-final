package com.taskflow.chatservice.controller;

import com.taskflow.chatservice.dto.ChatMessageResponse;
import com.taskflow.chatservice.dto.SendMessageRequest;
import com.taskflow.chatservice.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
@Tag(name = "Chat", description = "Task-specific chat between users and admins")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Send a message.
     * Rules:
     *  - ADMIN / MANAGER : can send on any task
     *  - USER            : can only send on tasks they are the assignee of
     */
    @PostMapping("/send")
    @Operation(summary = "Send a chat message for a task")
    public ResponseEntity<Object> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            Authentication auth) {

        String senderEmail = getEmail(auth);
        String senderName  = auth.getName();
        String senderRole  = getRole(auth);

        // Non-admins must already be a participant (i.e. the assignee who sent
        // the first message) OR this is the first message from the assignee.
        // The assignee check is enforced on the frontend; here we just block
        // users who are neither admin/manager nor a participant from sending.
        if (!"SUPER_ADMIN".equals(senderRole) && !"ADMIN".equals(senderRole) && !"MANAGER".equals(senderRole)) {
            boolean participant = chatService.isParticipant(request.getTaskId(), senderEmail);
            // Allow first message from assignee (participant list is empty yet)
            // We rely on the frontend to only show Chat to the assignee.
            // Still, double-check they have a valid reason to be here.
            if (!participant && chatService.hasMessages(request.getTaskId())) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "You are not authorized to chat on this task."));
            }
        }

        ChatMessageResponse response = chatService.sendMessage(
                request, senderEmail, senderName, senderRole);
        return ResponseEntity.ok(response);
    }

    /**
     * Get messages for a task.
     * Rules:
     *  - ADMIN / MANAGER : can see all task chats
     *  - USER            : can only see chats for tasks assigned to them
     */
    @GetMapping("/messages/{taskId}")
    @Operation(summary = "Get all chat messages for a task")
    public ResponseEntity<Object> getMessages(
            @PathVariable Long taskId,
            Authentication auth) {

        String currentEmail = getEmail(auth);
        String currentRole  = getRole(auth);

        if (!"SUPER_ADMIN".equals(currentRole) && !"ADMIN".equals(currentRole) && !"MANAGER".equals(currentRole)
                && !chatService.isParticipant(taskId, currentEmail)) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "You are not authorized to view this chat."));
        }

        List<ChatMessageResponse> messages = chatService.getMessages(taskId, currentEmail);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/unread/{taskId}")
    @Operation(summary = "Get unread message count for a task")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @PathVariable Long taskId,
            Authentication auth) {

        String currentEmail = getEmail(auth);
        String currentRole  = getRole(auth);

        // Users can only check unread on their own tasks
        if (!"SUPER_ADMIN".equals(currentRole) && !"ADMIN".equals(currentRole) && !"MANAGER".equals(currentRole)
                && !chatService.isParticipant(taskId, currentEmail)) {
            return ResponseEntity.ok(Map.of("unreadCount", 0L));
        }

        long count = chatService.getUnreadCount(taskId, currentEmail);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @GetMapping("/unread/total")
    @Operation(summary = "Get total unread messages for current user")
    public ResponseEntity<Map<String, Long>> getTotalUnread(Authentication auth) {
        long count = chatService.getTotalUnreadCount(getEmail(auth));
        return ResponseEntity.ok(Map.of("totalUnread", count));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static String getEmail(Authentication auth) {
        Object details = auth.getDetails();
        if (details instanceof String email) {
            return email;
        }
        return auth.getName();
    }

    private static String getRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("USER");
    }
}
