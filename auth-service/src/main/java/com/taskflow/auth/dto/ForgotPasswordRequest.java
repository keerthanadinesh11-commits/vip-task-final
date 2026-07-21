package com.taskflow.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Request body for POST /auth/forgot-password */
public class ForgotPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    public ForgotPasswordRequest() {
        // Required for JSON deserialization by Spring/Jackson
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}