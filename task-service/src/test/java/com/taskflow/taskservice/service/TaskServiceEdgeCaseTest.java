package com.taskflow.taskservice.service;

import com.taskflow.taskservice.client.NotificationClient;
import com.taskflow.taskservice.entity.Task;
import com.taskflow.taskservice.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceEdgeCaseTest {

    @Mock TaskRepository repository;
    @Mock NotificationClient notificationClient;
    @InjectMocks TaskService service;

    @Test
    void assignTask_withoutAssignee_savesWithoutNotification() {
        Task t = new Task(null, "Unassigned", "", "PENDING", null);
        when(repository.save(any())).thenAnswer(inv -> {
            Task saved = inv.getArgument(0);
            saved.setId(10L);
            return saved;
        });
        // Pass null for file and fileStorageService (no file attached)
        Task saved = service.assignTask(t, null, null);
        assertNotNull(saved);
        verify(notificationClient, never()).sendNotification(any());
    }

    @Test
    void assignTask_blankAssignee_doesNotSendNotification() {
        Task t = new Task(null, "Task", "", "PENDING", "  ");
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.assignTask(t, null, null);
        verify(notificationClient, never()).sendNotification(any());
    }

    @Test
    void updateTask_adminCanReassign() {
        Task existing = new Task(1L, "T", "", "ASSIGNED", "alice@x.com");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Task patch = new Task(null, "T", "", "ASSIGNED", "bob@x.com");
        Task saved = service.updateTask(1L, patch, "admin@x.com", "ADMIN");
        assertEquals("bob@x.com", saved.getAssignee());
    }

    @Test
    void updateTask_userCannotReassign() {
        Task existing = new Task(1L, "T", "", "ASSIGNED", "alice@x.com");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Task patch = new Task(null, "T", "", "ASSIGNED", "bob@x.com");
        Task saved = service.updateTask(1L, patch, "alice@x.com", "USER");
        assertEquals("alice@x.com", saved.getAssignee());
    }

    @Test
    void updateTask_completedTask_setsCompletedTime() {
        Task existing = new Task(1L, "T", "", "IN_PROGRESS", "alice@x.com");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Task patch = new Task(null, "T", "", "COMPLETED", "alice@x.com");
        Task saved = service.updateTask(1L, patch, "alice@x.com", "USER");
        assertNotNull(saved.getCompletedTime());
    }

    @Test
    void canUpdate_nullAssigneeTaskAndUser_returnsFalse() {
        Task t = new Task(1L, "T", "", "PENDING", null);
        assertFalse(service.canUpdate(t, "anyone@x.com", "USER"));
    }

    @Test
    void canUpdate_managerRole_followsNonAdminPath() {
        Task t = new Task(1L, "T", "", "PENDING", "other@x.com");
        assertFalse(service.canUpdate(t, "mgr@x.com", "MANAGER"));
    }

    @Test
    void createTask_blankAssignee_skipsNotification() {
        Task t = new Task(null, "T", "", "PENDING", "");
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.createTask(t);
        verify(notificationClient, never()).sendNotification(any());
    }
}
