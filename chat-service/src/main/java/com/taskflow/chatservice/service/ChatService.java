package com.taskflow.chatservice.service;

import com.taskflow.chatservice.dto.ChatMessageResponse;
import com.taskflow.chatservice.dto.SendMessageRequest;
import com.taskflow.chatservice.entity.ChatMessage;
import com.taskflow.chatservice.repository.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final ChatMessageRepository chatRepository;

    public ChatService(ChatMessageRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    @Transactional
    public ChatMessageResponse sendMessage(SendMessageRequest request,
                                           String senderEmail,
                                           String senderName,
                                           String senderRole) {
        ChatMessage message = new ChatMessage(
                request.getTaskId(),
                senderEmail,
                senderName,
                senderRole,
                request.getContent().trim()
        );
        ChatMessage saved = chatRepository.save(message);
        logger.info("[ChatService] Message sent for taskId={} by={}", request.getTaskId(), senderEmail);
        return ChatMessageResponse.from(saved);
    }

    @Transactional
    public List<ChatMessageResponse> getMessages(Long taskId, String currentEmail) {
        chatRepository.markAllAsReadForTask(taskId, currentEmail);
        return chatRepository.findByTaskIdOrderByTimestampAsc(taskId)
                .stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    public boolean isParticipant(Long taskId, String email) {
        return chatRepository.existsByTaskIdAndSenderEmail(taskId, email);
    }

    public long getUnreadCount(Long taskId, String currentEmail) {
        return chatRepository.countUnreadByTaskIdAndNotSender(taskId, currentEmail);
    }

    public long getTotalUnreadCount(String currentEmail) {
        return chatRepository.countAllUnreadForUser(currentEmail);
    }

    /** Returns true if any messages exist for this task (used for access control). */
    public boolean hasMessages(Long taskId) {
        return chatRepository.existsById(taskId);
    }
}
