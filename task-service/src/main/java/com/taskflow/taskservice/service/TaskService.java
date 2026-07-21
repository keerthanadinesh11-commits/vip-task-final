package com.taskflow.taskservice.service;

import com.taskflow.taskservice.client.NotificationClient;
import com.taskflow.taskservice.dto.NotificationDto;
import com.taskflow.taskservice.entity.Task;
import com.taskflow.taskservice.exception.ForbiddenException;
import com.taskflow.taskservice.exception.TaskNotFoundException;
import com.taskflow.taskservice.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    static final String STATUS_ASSIGNED  = "ASSIGNED";
    static final String STATUS_COMPLETED = "COMPLETED";
    static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    static final String ROLE_ADMIN       = "ADMIN";
    static final String ROLE_MANAGER     = "MANAGER";

    private final TaskRepository repository;
    private final NotificationClient notificationClient;

    public TaskService(TaskRepository repository, NotificationClient notificationClient) {
        this.repository = repository;
        this.notificationClient = notificationClient;
    }

    public List<Task> getAll() { return repository.findAll(); }

    public Task getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    @Transactional
    public Task createTask(Task task) {
        Task saved = repository.save(task);
        if (hasAssignee(saved)) {
            try {
                sendNotification(saved.getAssignee(),
                        saved.getTitle() + " is assigned to " + saved.getAssignee());
            } catch (Exception ex) {
                log.warn("[TaskService] Notification failed for createTask: {}", ex.getMessage());
            }
        }
        return saved;
    }

    @Transactional
    public Task assignTask(Task task, MultipartFile file, FileStorageService fileStorageService) {
        task.setStatus(STATUS_ASSIGNED);

        if (file != null && !file.isEmpty()) {
            String storedName = fileStorageService.storeFile(file);
            task.setAssignedFileName(storedName);
            task.setAssignedFileOriginalName(file.getOriginalFilename());
        }

        Task saved = repository.save(task);
        if (hasAssignee(saved)) {
            String msg = saved.getTitle() + " is assigned to " + saved.getAssignee();
            if (file != null && !file.isEmpty()) {
                msg += ". A file has been attached for you to work on.";
            }
            try {
                sendNotification(saved.getAssignee(), msg);
            } catch (Exception ex) {
                log.warn("[TaskService] Notification failed for assignTask: {}", ex.getMessage());
            }
        }
        return saved;
    }

    @Transactional
    public Task uploadCompletedFile(Long taskId, MultipartFile file, String callerEmail,
                                     String callerRole, FileStorageService fileStorageService) {
        Task task = repository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        log.info("[Upload] taskId={} assignee='{}' callerEmail='{}' callerRole='{}'",
                taskId, task.getAssignee(), callerEmail, callerRole);

        if (!canUpdate(task, callerEmail, callerRole)) {
            log.warn("[Upload] FORBIDDEN — assignee='{}' != callerEmail='{}'",
                    task.getAssignee(), callerEmail);
            throw new ForbiddenException(
                    "Only the assignee (" + task.getAssignee() + ") or an administrator may upload. " +
                    "Your email: " + callerEmail);
        }

        if (task.getCompletedFileName() != null) {
            fileStorageService.deleteFile(task.getCompletedFileName());
        }

        String storedName = fileStorageService.storeFile(file);
        task.setCompletedFileName(storedName);
        task.setCompletedFileOriginalName(file.getOriginalFilename());

        Task saved = repository.save(task);

        // Send notification best-effort — never fail the upload if notification-service is down
        try {
            sendNotification("admin",
                    callerEmail + " has uploaded completed work for: " + task.getTitle());
        } catch (Exception ex) {
            log.warn("[TaskService] Notification failed for upload (task={}): {}", taskId, ex.getMessage());
        }

        return saved;
    }

    @Transactional
    public Task updateTask(Long id, Task updated, String callerEmail, String callerRole) {
        Task existing = repository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        authorizeUpdate(existing, callerEmail, callerRole);

        boolean transitioningToCompleted = !STATUS_COMPLETED.equals(existing.getStatus())
                && STATUS_COMPLETED.equals(updated.getStatus());

        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setStatus(updated.getStatus());
        if (ROLE_SUPER_ADMIN.equals(callerRole) || ROLE_ADMIN.equals(callerRole) || ROLE_MANAGER.equals(callerRole)) {
            existing.setAssignee(updated.getAssignee());
        }

        if (transitioningToCompleted) {
            existing.setCompletedTime(LocalDateTime.now());
        }
        Task saved = repository.save(existing);

        if (transitioningToCompleted && hasAssignee(saved)) {
            try {
                sendNotification(saved.getAssignee(), saved.getTitle() + " is completed");
            } catch (Exception ex) {
                log.warn("[TaskService] Notification failed for updateTask: {}", ex.getMessage());
            }
        }
        return saved;
    }

    @Transactional
    public Task deleteCompletedFile(Long taskId, FileStorageService fileStorageService) {
        Task task = repository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        if (task.getCompletedFileName() != null) {
            fileStorageService.deleteFile(task.getCompletedFileName());
            task.setCompletedFileName(null);
            task.setCompletedFileOriginalName(null);
        }
        return repository.save(task);
    }

    /**
     * ADMIN and MANAGER can always update/upload.
     * USER can update/upload only if they are the assignee.
     * Comparison is case-insensitive and trims whitespace.
     */
    public boolean canUpdate(Task task, String callerEmail, String callerRole) {
        if (ROLE_SUPER_ADMIN.equals(callerRole) || ROLE_ADMIN.equals(callerRole) || ROLE_MANAGER.equals(callerRole)) return true;
        if (task.getAssignee() == null || callerEmail == null) return false;
        return task.getAssignee().trim().equalsIgnoreCase(callerEmail.trim());
    }

    private void authorizeUpdate(Task task, String callerEmail, String callerRole) {
        if (!canUpdate(task, callerEmail, callerRole)) {
            throw new ForbiddenException("Only the assignee or an administrator may update this task");
        }
    }

    private boolean hasAssignee(Task task) {
        return task.getAssignee() != null && !task.getAssignee().isBlank();
    }

    private void sendNotification(String userId, String message) {
        notificationClient.sendNotification(new NotificationDto(message, userId));
    }
}
