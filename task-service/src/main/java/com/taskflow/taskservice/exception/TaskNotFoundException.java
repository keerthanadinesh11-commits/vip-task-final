package com.taskflow.taskservice.exception;

/**
 * Thrown when a referenced task does not exist. Mapped to HTTP 404.
 */
public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(Long id) {
        super("Task not found with id: " + id);
    }
}
