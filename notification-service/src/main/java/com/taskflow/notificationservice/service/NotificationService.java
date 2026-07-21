package com.taskflow.notificationservice.service;

import com.taskflow.notificationservice.entity.Notification;
import com.taskflow.notificationservice.repository.NotificationRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    public Notification save(Notification notification) {
        return repository.save(notification);
    }

    public List<Notification> getAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "timestamp"));
    }

    public List<Notification> getForUser(String userId) {
        return repository.findByUserIdOrderByTimestampDesc(userId);
    }

    public Notification findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with id: " + id));
    }
}
