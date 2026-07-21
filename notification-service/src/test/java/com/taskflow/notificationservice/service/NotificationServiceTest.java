package com.taskflow.notificationservice.service;

import com.taskflow.notificationservice.entity.Notification;
import com.taskflow.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository repository;
    @InjectMocks private NotificationService service;

    @Test
    void savePersistsNotification() {
        Notification n = new Notification(null, "Task assigned", "alice@x.com", null);
        when(repository.save(n)).thenAnswer(inv -> {
            n.setId(1L);
            return n;
        });
        Notification saved = service.save(n);
        assertEquals(1L, saved.getId());
        verify(repository).save(n);
    }

    @Test
    void getAllReturnsSortedList() {
        Notification n1 = new Notification(1L, "msg1", "a@x.com", LocalDateTime.now());
        Notification n2 = new Notification(2L, "msg2", "b@x.com", LocalDateTime.now());
        when(repository.findAll(any(Sort.class))).thenReturn(List.of(n2, n1));
        List<Notification> result = service.getAll();
        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).getId());
    }

    @Test
    void getForUserFiltersById() {
        Notification n = new Notification(1L, "msg", "alice@x.com", LocalDateTime.now());
        when(repository.findByUserIdOrderByTimestampDesc("alice@x.com")).thenReturn(List.of(n));
        List<Notification> result = service.getForUser("alice@x.com");
        assertEquals(1, result.size());
        assertEquals("alice@x.com", result.get(0).getUserId());
    }

    @Test
    void findByIdReturnsNotification() {
        Notification n = new Notification(5L, "hello", "u@x.com", LocalDateTime.now());
        when(repository.findById(5L)).thenReturn(Optional.of(n));
        assertEquals("hello", service.findById(5L).getMessage());
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.findById(99L));
    }
}
