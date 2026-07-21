package com.taskflow.auth.controller;

import com.taskflow.auth.dto.AdminCreateUserRequest;
import com.taskflow.auth.dto.AuthRequest;
import com.taskflow.auth.dto.AuthResponse;
import com.taskflow.auth.dto.ManagerSummary;
import com.taskflow.auth.dto.PasswordChangeRequest;
import com.taskflow.auth.dto.RegisterRequest;
import com.taskflow.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Registration, login, password and admin user management")
public class AuthController {

    private final AuthService service;
    private final AuthenticationManager authenticationManager;

    public AuthController(AuthService service, AuthenticationManager authenticationManager) {
        this.service = service;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    @Operation(summary = "Self-service registration",
            description = "Creates a new USER account and returns a confirmation message.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registration successful"),
            @ApiResponse(responseCode = "400", description = "Validation failed or duplicate user")
    })
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        String message = service.register(request);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive a JWT",
            description = "Returns a signed JWT plus the username and role of the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        // Give specific error messages: distinguish missing email from wrong password
        service.validateLoginCredentials(request.getEmail(), request.getPassword());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        AuthResponse response = service.generateTokenResponseByEmail(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/users")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Create a user as administrator",
            description = "Admin-only endpoint that creates the credentials in auth-service and "
                    + "synchronises the profile to user-service so the new user can log in immediately.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created"),
            @ApiResponse(responseCode = "400", description = "Validation failed or duplicate user"),
            @ApiResponse(responseCode = "403", description = "Caller is not an administrator")
    })
    public ResponseEntity<AuthResponse> adminCreateUser(@Valid @RequestBody AdminCreateUserRequest request) {
        AuthResponse response = service.adminCreateUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/managers")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "List managers (optionally by status)",
            description = "Super-admin view of manager accounts. Pass ?status=PENDING to see "
                    + "the approval queue, or omit status for all managers.")
    public ResponseEntity<List<ManagerSummary>> listManagers(
            @RequestParam(value = "status", required = false) String status) {
        return ResponseEntity.ok(service.listManagers(status));
    }

    @PutMapping("/managers/{id}/approve")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Approve a pending manager",
            description = "Marks the manager APPROVED so they can log in.")
    public ResponseEntity<ManagerSummary> approveManager(@PathVariable("id") Long id) {
        return ResponseEntity.ok(service.approveManager(id));
    }

    @PutMapping("/managers/{id}/reject")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Reject a pending manager",
            description = "Marks the manager REJECTED; they remain unable to log in.")
    public ResponseEntity<ManagerSummary> rejectManager(@PathVariable("id") Long id) {
        return ResponseEntity.ok(service.rejectManager(id));
    }

    @GetMapping("/managers/approved")
    @Operation(summary = "List approved managers by department",
            description = "Public lookup used by the user-registration screen. Pass "
                    + "?department=SALES to filter, or omit for all approved managers.")
    public ResponseEntity<List<ManagerSummary>> listApprovedManagers(
            @RequestParam(value = "department", required = false) String department) {
        return ResponseEntity.ok(service.listApprovedManagers(department));
    }

    @PutMapping("/password")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Change own password",
            description = "Authenticated users may rotate their password if they supply the current one.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password updated"),
            @ApiResponse(responseCode = "400", description = "Current password incorrect or new password invalid"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    })
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody PasswordChangeRequest request,
            Authentication authentication) {
        service.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    /**
     * OTP-verified password update — no current-password check required.
     * Called from the Profile → Change Password flow after OTP is verified.
     */
    @PutMapping("/password/direct")
    public ResponseEntity<Map<String, String>> updatePasswordDirect(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "New password is required"));
        }
        service.updatePasswordDirect(authentication.getName(), newPassword);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

}