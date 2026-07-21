package com.taskflow.auth.service;

import com.taskflow.auth.client.UserServiceClient;
import com.taskflow.auth.dto.AdminCreateUserRequest;
import com.taskflow.auth.dto.AuthResponse;
import com.taskflow.auth.dto.ManagerSummary;
import com.taskflow.auth.dto.PasswordChangeRequest;
import com.taskflow.auth.dto.RegisterRequest;
import com.taskflow.auth.entity.UserCredential;
import com.taskflow.auth.exception.InvalidCredentialsException;
import com.taskflow.auth.repository.UserCredentialRepository;
import com.taskflow.auth.util.PasswordPolicy;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Core authentication service.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    static final String ROLE_MANAGER     = "MANAGER";
    static final String ROLE_USER        = "USER";

    static final String STATUS_PENDING   = "PENDING";
    static final String STATUS_APPROVED  = "APPROVED";
    static final String STATUS_REJECTED  = "REJECTED";

    private static final String USER_NOT_FOUND = "User not found";

    private final UserCredentialRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserServiceClient userServiceClient;

    @Value("${app.default-admin-email:admin@taskflow.com}")
    private String defaultAdminEmail;

    @Value("${app.default-admin-password:Admin123}")
    private String defaultAdminPassword;

    public AuthService(UserCredentialRepository repository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       UserServiceClient userServiceClient) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userServiceClient = userServiceClient;
    }

    @PostConstruct
    public void seedDefaultAdmin() {
        if (repository.existsByEmail(defaultAdminEmail)) {
            return;
        }
        UserCredential admin = new UserCredential();
        admin.setUsername(ROLE_SUPER_ADMIN);
        admin.setEmail(defaultAdminEmail);
        admin.setPassword(passwordEncoder.encode(defaultAdminPassword));
        admin.setRole(ROLE_SUPER_ADMIN);
        admin.setStatus(STATUS_APPROVED);
        repository.save(admin);
        log.info("Default super-administrator account initialised");
        syncProfileSafe(ROLE_SUPER_ADMIN, defaultAdminEmail, ROLE_SUPER_ADMIN, STATUS_APPROVED, null, null);
    }

    @Transactional
    public String register(RegisterRequest request) {
        validatePassword(request.getPassword());
        ensureUnique(request.getEmail());

        String requestedRole = request.getRole() == null
                ? ROLE_USER : request.getRole().toUpperCase();
        String department = request.getDepartment() == null
                ? null : request.getDepartment().toUpperCase();

        UserCredential credential = new UserCredential();
        credential.setUsername(request.getUsername());
        credential.setEmail(request.getEmail());
        credential.setPassword(passwordEncoder.encode(request.getPassword()));
        credential.setDepartment(department);

        if (ROLE_MANAGER.equals(requestedRole)) {
            // Managers require super-admin approval before they can log in.
            credential.setRole(ROLE_MANAGER);
            credential.setStatus(STATUS_PENDING);
            repository.save(credential);
            syncProfileSafe(request.getUsername(), request.getEmail(),
                    ROLE_MANAGER, STATUS_PENDING, department, null);
            return "Manager registration submitted. An administrator must approve "
                    + "your account before you can log in.";
        }

        // Regular user: active immediately, auto-assigned to the department's approved manager.
        credential.setRole(ROLE_USER);
        credential.setStatus(STATUS_APPROVED);
        String managerEmail = resolveDepartmentManagerEmail(department);
        credential.setManagerEmail(managerEmail);
        repository.save(credential);

        syncProfileSafe(request.getUsername(), request.getEmail(),
                ROLE_USER, STATUS_APPROVED, department, managerEmail);
        return "User registered successfully";
    }

    /**
     * Returns the email of the first APPROVED manager for the given department,
     * or null if none exists yet (the user is still created and can be reassigned later).
     */
    private String resolveDepartmentManagerEmail(String department) {
        if (department == null) {
            return null;
        }
        return repository.findByRoleAndStatusAndDepartment(ROLE_MANAGER, STATUS_APPROVED, department)
                .stream()
                .map(UserCredential::getEmail)
                .findFirst()
                .orElse(null);
    }

    // ── Super-admin: manager approval workflow ───────────────────────────────

    public List<ManagerSummary> listManagers(String status) {
        List<UserCredential> managers = (status == null || status.isBlank())
                ? repository.findByRole(ROLE_MANAGER)
                : repository.findByRoleAndStatus(ROLE_MANAGER, status.toUpperCase());
        return managers.stream()
                .map(m -> new ManagerSummary(m.getId(), m.getUsername(), m.getEmail(),
                        m.getDepartment(),
                        m.getStatus() == null ? STATUS_PENDING : m.getStatus()))
                .collect(Collectors.toList());
    }

    /** Approved managers for a department — feeds the user-registration screen. */
    public List<ManagerSummary> listApprovedManagers(String department) {
        List<UserCredential> managers = (department == null || department.isBlank())
                ? repository.findByRoleAndStatus(ROLE_MANAGER, STATUS_APPROVED)
                : repository.findByRoleAndStatusAndDepartment(ROLE_MANAGER, STATUS_APPROVED,
                        department.toUpperCase());
        return managers.stream()
                .map(m -> new ManagerSummary(m.getId(), m.getUsername(), m.getEmail(),
                        m.getDepartment(), m.getStatus()))
                .collect(Collectors.toList());
    }

    @Transactional
    public ManagerSummary approveManager(Long managerId) {
        UserCredential manager = requireManager(managerId);
        manager.setStatus(STATUS_APPROVED);
        repository.save(manager);
        syncStatusSafe(manager.getEmail(), STATUS_APPROVED);
        return new ManagerSummary(manager.getId(), manager.getUsername(), manager.getEmail(),
                manager.getDepartment(), manager.getStatus());
    }

    @Transactional
    public ManagerSummary rejectManager(Long managerId) {
        UserCredential manager = requireManager(managerId);
        manager.setStatus(STATUS_REJECTED);
        repository.save(manager);
        syncStatusSafe(manager.getEmail(), STATUS_REJECTED);
        return new ManagerSummary(manager.getId(), manager.getUsername(), manager.getEmail(),
                manager.getDepartment(), manager.getStatus());
    }

    private UserCredential requireManager(Long managerId) {
        UserCredential manager = repository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("Manager not found"));
        if (!ROLE_MANAGER.equals(manager.getRole())) {
            throw new IllegalArgumentException("Account is not a manager");
        }
        return manager;
    }

    @Transactional
    public AuthResponse adminCreateUser(AdminCreateUserRequest request) {
        validatePassword(request.getPassword());
        ensureUnique(request.getEmail());

        String role = request.getRole() == null ? ROLE_USER : request.getRole().toUpperCase();
        UserCredential credential = new UserCredential();
        credential.setUsername(request.getUsername());
        credential.setEmail(request.getEmail());
        credential.setPassword(passwordEncoder.encode(request.getPassword()));
        credential.setRole(role);
        // Admin-created accounts are trusted and active immediately.
        credential.setStatus(STATUS_APPROVED);
        repository.save(credential);

        syncProfileSafe(request.getUsername(), request.getEmail(), role,
                STATUS_APPROVED, null, null);
        return new AuthResponse(null, credential.getUsername(), credential.getRole());
    }

    @Transactional
    public void changePassword(String email, PasswordChangeRequest request) {
        validatePassword(request.getNewPassword());
        UserCredential credential = repository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND));
        if (!passwordEncoder.matches(request.getCurrentPassword(), credential.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        credential.setPassword(passwordEncoder.encode(request.getNewPassword()));
        repository.save(credential);
    }

    public AuthResponse generateTokenResponseByEmail(String email) {
        UserCredential credential = repository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND));
        String token = jwtService.generateToken(credential);
        // Record last login in user-service (best-effort)
        try {
            java.util.Map<String, String> body = new java.util.HashMap<>();
            body.put("email", email);
            userServiceClient.recordLogin(body);
        } catch (Exception ex) {
            log.warn("Could not record last login: {}", ex.getMessage());
        }
        return new AuthResponse(token, credential.getUsername(), credential.getRole());
    }

    /**
     * Validates login credentials. Throws {@link InvalidCredentialsException} with
     * specific messages to distinguish wrong email vs wrong password, and blocks
     * managers whose account has not yet been approved by a super-admin.
     */
    public void validateLoginCredentials(String email, String password) {
        UserCredential credential = repository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Incorrect email address"));
        if (!passwordEncoder.matches(password, credential.getPassword())) {
            throw new InvalidCredentialsException("Incorrect password");
        }
        // Approval gate: managers cannot log in until approved.
        if (ROLE_MANAGER.equals(credential.getRole())
                && !STATUS_APPROVED.equals(credential.getStatus())) {
            if (STATUS_REJECTED.equals(credential.getStatus())) {
                throw new InvalidCredentialsException(
                        "Your manager registration was rejected. Please contact an administrator.");
            }
            throw new InvalidCredentialsException(
                    "Your manager account is pending administrator approval.");
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void validatePassword(String password) {
        if (!PasswordPolicy.isValid(password)) {
            throw new IllegalArgumentException(PasswordPolicy.MESSAGE);
        }
    }

    private void ensureUnique(String email) {
        if (repository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }
    }

    private void syncProfileSafe(String username, String email, String role,
                                 String status, String department, String managerEmail) {
        try {
            Map<String, String> profile = new HashMap<>();
            profile.put("username", username);
            profile.put("email", email);
            profile.put("role", role);
            profile.put("status", status);
            if (department != null)   profile.put("department", department);
            if (managerEmail != null) profile.put("managerEmail", managerEmail);
            userServiceClient.createUserProfile(profile);
        } catch (Exception ex) {
            log.warn("Profile sync to user-service failed: {}", ex.getMessage());
        }
    }

    private void syncStatusSafe(String email, String status) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("email", email);
            body.put("status", status);
            userServiceClient.updateStatus(body);
        } catch (Exception ex) {
            log.warn("Status sync to user-service failed: {}", ex.getMessage());
        }
    }

    /**
     * Updates password directly (no current-password check).
     * Called after OTP has been verified via user-service.
     */
    @Transactional
    public void updatePasswordDirect(String email, String newPassword) {
        validatePassword(newPassword);
        UserCredential credential = repository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND));
        credential.setPassword(passwordEncoder.encode(newPassword));
        repository.save(credential);
    }

}
