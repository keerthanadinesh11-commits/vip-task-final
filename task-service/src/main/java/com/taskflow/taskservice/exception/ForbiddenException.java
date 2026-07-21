package com.taskflow.taskservice.exception;

/**
 * Thrown when an authenticated caller is not allowed to perform the requested
 * action on a task. Mapped to HTTP 403 by the global exception handler.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
