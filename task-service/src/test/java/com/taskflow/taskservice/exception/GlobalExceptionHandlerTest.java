package com.taskflow.taskservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleTaskNotFound_returns404() {
        ResponseEntity<Map<String, String>> response =
                handler.handleNotFound(new TaskNotFoundException(42L));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Task not found with id: 42", response.getBody().get("error"));
    }

    @Test
    void handleForbidden_returns403() {
        ResponseEntity<Map<String, String>> response =
                handler.handleForbidden(new ForbiddenException("not your task"));
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("not your task", response.getBody().get("error"));
    }

    @Test
    void handleAccessDenied_returns403() {
        ResponseEntity<Map<String, String>> response =
                handler.handleAccessDenied(new AccessDeniedException("denied"));
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Access denied", response.getBody().get("error"));
    }

    @Test
    void handleIllegalArgument_returns400() {
        ResponseEntity<Map<String, String>> response =
                handler.handleIllegalArgument(new IllegalArgumentException("bad input"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("bad input", response.getBody().get("error"));
    }

    @Test
    void handleGenericException_returns500() {
        ResponseEntity<Map<String, String>> response =
                handler.handleException(new RuntimeException("unexpected"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred", response.getBody().get("error"));
    }

    @Test
    void handleValidation_returns400WithFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult binding = mock(BindingResult.class);
        FieldError fieldError = new FieldError("dto", "title", "Title is required");
        when(ex.getBindingResult()).thenReturn(binding);
        when(binding.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<Map<String, String>> response = handler.handleValidation(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Title is required", response.getBody().get("title"));
    }

    @Test
    void taskNotFoundException_message_containsId() {
        TaskNotFoundException ex = new TaskNotFoundException(7L);
        assertEquals("Task not found with id: 7", ex.getMessage());
    }

    @Test
    void forbiddenException_message_isPreserved() {
        ForbiddenException ex = new ForbiddenException("custom forbidden");
        assertEquals("custom forbidden", ex.getMessage());
    }
}
