package com.taskflow.taskservice.service;

import com.taskflow.taskservice.client.NotificationClient;
import com.taskflow.taskservice.dto.NotificationDto;
import com.taskflow.taskservice.entity.Task;
import com.taskflow.taskservice.exception.ForbiddenException;
import com.taskflow.taskservice.exception.TaskNotFoundException;
import com.taskflow.taskservice.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository repository;
    @Mock private NotificationClient notificationClient;
    @InjectMocks private TaskService service;

    @Test
    void createTaskSendsAssignmentNotification() {
        Task t = new Task(null, "Fix bug", "desc", "ASSIGNED", "alice@x.com");
        when(repository.save(t)).thenAnswer(inv -> { t.setId(1L); return t; });
        Task saved = service.createTask(t);
        assertEquals(1L, saved.getId());
        verify(notificationClient, times(1)).sendNotification(any(NotificationDto.class));
    }

    @Test
    void createTaskWithoutAssigneeSkipsNotification() {
        Task t = new Task(null, "Floating task", "", "PENDING", null);
        when(repository.save(t)).thenAnswer(inv -> { t.setId(2L); return t; });
        service.createTask(t);
        verify(notificationClient, never()).sendNotification(any());
    }

    @Test
    void assignTaskForcesStatusAndNotifies() {
        Task t = new Task(null, "Task", "", "PENDING", "bob@x.com");
        when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        // assignTask now takes (Task, MultipartFile, FileStorageService) — pass nulls for no file
        Task saved = service.assignTask(t, null, null);
        assertEquals("ASSIGNED", saved.getStatus());
        verify(notificationClient).sendNotification(any());
    }

    @Test
    void notificationFailureDoesNotBubbleUp() {
        Task t = new Task(null, "T", "", "ASSIGNED", "a@x.com");
        when(repository.save(t)).thenAnswer(inv -> { t.setId(5L); return t; });
        doThrow(new RuntimeException("notif down")).when(notificationClient).sendNotification(any());
        assertNotNull(service.createTask(t));
    }

    @Test
    void getAllReturnsRepositoryList() {
        when(repository.findAll()).thenReturn(List.of(new Task(1L, "a", "", "PENDING", null)));
        assertEquals(1, service.getAll().size());
    }

    @Test
    void getByIdThrowsWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(TaskNotFoundException.class, () -> service.getById(99L));
    }

    @Test
    void updateThrowsWhenTaskMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        Task patch = new Task(null, "x", "", "PENDING", null);
        assertThrows(TaskNotFoundException.class,
                () -> service.updateTask(99L, patch, "u@x.com", "USER"));
    }

    @Test
    void updateForbiddenForOtherUser() {
        Task existing = new Task(1L, "T", "", "ASSIGNED", "alice@x.com");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        Task patch = new Task(null, "T2", "", "ASSIGNED", "alice@x.com");
        assertThrows(ForbiddenException.class,
                () -> service.updateTask(1L, patch, "bob@x.com", "USER"));
    }

    @Test
    void updateAllowedForAssignee() {
        Task existing = new Task(1L, "T", "", "ASSIGNED", "alice@x.com");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        Task patch = new Task(null, "T", "", "IN_PROGRESS", "alice@x.com");
        Task saved = service.updateTask(1L, patch, "alice@x.com", "USER");
        assertEquals("IN_PROGRESS", saved.getStatus());
    }

    @Test
    void updateAllowedForAdminEvenIfNotAssignee() {
        Task existing = new Task(1L, "T", "", "ASSIGNED", "alice@x.com");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        Task patch = new Task(null, "T", "", "COMPLETED", "alice@x.com");
        Task saved = service.updateTask(1L, patch, "admin@x.com", "ADMIN");
        assertEquals("COMPLETED", saved.getStatus());
        verify(notificationClient).sendNotification(any());
    }

    @Test
    void updateToCompletedFiresCompletionNotification() {
        Task existing = new Task(1L, "T", "", "IN_PROGRESS", "a@x.com");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        Task patch = new Task(null, "T", "", "COMPLETED", "a@x.com");
        service.updateTask(1L, patch, "a@x.com", "USER");
        verify(notificationClient).sendNotification(any());
    }

    @Test
    void updateAlreadyCompletedDoesNotFireNotificationAgain() {
        Task existing = new Task(1L, "T", "", "COMPLETED", "a@x.com");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        Task patch = new Task(null, "T", "", "COMPLETED", "a@x.com");
        service.updateTask(1L, patch, "a@x.com", "USER");
        verify(notificationClient, never()).sendNotification(any());
    }

    @Test
    void canUpdateMatchesAuthorizationLogic() {
        Task t = new Task(1L, "T", "", "ASSIGNED", "alice@x.com");
        assertTrue(service.canUpdate(t, "anybody@x.com", "ADMIN"));
        assertTrue(service.canUpdate(t, "alice@x.com", "USER"));
        assertFalse(service.canUpdate(t, "bob@x.com", "USER"));
        assertFalse(service.canUpdate(new Task(2L, "T", "", "PENDING", null), "anyone@x.com", "USER"));
    }
}
