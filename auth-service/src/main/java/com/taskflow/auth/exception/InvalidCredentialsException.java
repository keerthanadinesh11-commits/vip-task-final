package com.taskflow.auth.exception;

/**
 * Thrown when login credentials are invalid.
 * Carries a specific message ("Incorrect email address" or "Incorrect password")
 * so the client can show a precise error.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
