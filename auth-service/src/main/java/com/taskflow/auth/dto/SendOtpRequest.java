package com.taskflow.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /auth/send-otp
 *
 * <p>Alias of ForgotPasswordRequest — provides a cleaner API endpoint name.
 * The /send-otp endpoint can be used for both forgot-password and
 * registration email verification in the future.
 */
public class SendOtpRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    /** Required for JSON deserialization by Spring/Jackson. */
    public SendOtpRequest() {}

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
