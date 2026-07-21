package com.taskflow.chatservice.repository;

import com.taskflow.chatservice.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository for chat message database operations.
 */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Get all messages for a specific task, ordered by time (oldest first).
     */
    List<ChatMessage> findByTaskIdOrderByTimestampAsc(Long taskId);

    /**
     * Count unread messages for a specific task not sent by the current user.
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.taskId = :taskId " +
           "AND m.isRead = false AND m.senderEmail <> :email")
    long countUnreadByTaskIdAndNotSender(@Param("taskId") Long taskId,
                                         @Param("email") String email);

    /**
     * Mark all messages in a task as read when a user opens the chat.
     * Only marks messages NOT sent by the current user as read.
     */
    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.taskId = :taskId " +
           "AND m.senderEmail <> :email")
    void markAllAsReadForTask(@Param("taskId") Long taskId,
                              @Param("email") String email);

    /**
     * Count total unread messages across all tasks for a user.
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.isRead = false " +
           "AND m.senderEmail <> :email")
    long countAllUnreadForUser(@Param("email") String email);

    /**
     * Check whether a user has ever sent a message on a given task.
     * Used to verify the user is the assignee/participant before granting chat access.
     */
    @Query("SELECT COUNT(m) > 0 FROM ChatMessage m WHERE m.taskId = :taskId " +
           "AND m.senderEmail = :email")
    boolean existsByTaskIdAndSenderEmail(@Param("taskId") Long taskId,
                                          @Param("email") String email);
}
