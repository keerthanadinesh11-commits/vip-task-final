package com.taskflow.auth.dto;

import com.taskflow.auth.util.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Self-service registration payload.
 *
 * Supports two flows selected by {@code role}:
 *   USER    -> account is active immediately; {@code department} chooses which
 *              department (and therefore which manager) the user reports to.
 *   MANAGER -> account is created PENDING and cannot log in until a SUPER_ADMIN
 *              approves it; {@code department} is the department they will manage.
 */
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE)
    private String password;

    /** Requested role: only USER or MANAGER may self-register. Defaults to USER. */
    @Pattern(regexp = "USER|MANAGER", message = "Role must be USER or MANAGER")
    private String role = "USER";

    /** Department the account belongs to. Required for both USER and MANAGER. */
    @NotBlank(message = "Department is required")
    private String department;

    public RegisterRequest() {
        // Default constructor required for JSON deserialization
    }

    public RegisterRequest(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
}
