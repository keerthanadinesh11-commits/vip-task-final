package com.taskflow.auth.service;

import com.taskflow.auth.client.UserServiceClient;
import com.taskflow.auth.dto.AdminCreateUserRequest;
import com.taskflow.auth.dto.AuthResponse;
import com.taskflow.auth.dto.PasswordChangeRequest;
import com.taskflow.auth.dto.RegisterRequest;
import com.taskflow.auth.entity.UserCredential;
import com.taskflow.auth.repository.UserCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserCredentialRepository repository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private UserServiceClient userServiceClient;

    @InjectMocks private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "defaultAdminEmail", "admin@taskflow.com");
        ReflectionTestUtils.setField(authService, "defaultAdminPassword", "Admin123");
    }

    @Test
    void seedDefaultAdminCreatesAccountWhenMissing() {
        when(repository.existsByEmail("admin@taskflow.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        authService.seedDefaultAdmin();

        verify(repository).save(any(UserCredential.class));
        verify(userServiceClient).createUserProfile(any());
    }

    @Test
    void seedDefaultAdminSkipsWhenPresent() {
        when(repository.existsByEmail("admin@taskflow.com")).thenReturn(true);

        authService.seedDefaultAdmin();

        verify(repository, never()).save(any());
        verify(userServiceClient, never()).createUserProfile(any());
    }

    @Test
    void registerHashesPasswordAndSyncsProfile() {
        RegisterRequest req = new RegisterRequest("john", "john@x.com", "Abcd1!");
        when(repository.existsByEmail("john@x.com")).thenReturn(false);
        when(passwordEncoder.encode("Abcd1!")).thenReturn("hashed");

        String result = authService.register(req);

        assertEquals("User registered successfully", result);
        verify(repository).save(any(UserCredential.class));
        verify(userServiceClient).createUserProfile(any());
    }

    @Test
    void registerRejectsWeakPassword() {
        RegisterRequest req = new RegisterRequest("john", "john@x.com", "weak");
        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
        verify(repository, never()).save(any());
    }

    @Test
    void registerAllowsDuplicateUsername() {
        // Usernames are no longer required to be unique; only email is unique
        RegisterRequest req = new RegisterRequest("john", "john@x.com", "Abcd1!");
        when(repository.existsByEmail("john@x.com")).thenReturn(false);
        when(passwordEncoder.encode("Abcd1!")).thenReturn("hashed");

        String result = authService.register(req);

        assertEquals("User registered successfully", result);
        verify(repository).save(any(UserCredential.class));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest req = new RegisterRequest("john", "john@x.com", "Abcd1!");
        when(repository.findByUsername("john")).thenReturn(Optional.empty());
        when(repository.existsByEmail("john@x.com")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
    }

    @Test
    void registerSucceedsEvenIfProfileSyncFails() {
        RegisterRequest req = new RegisterRequest("john", "john@x.com", "Abcd1!");
        when(repository.existsByEmail("john@x.com")).thenReturn(false);
        when(passwordEncoder.encode("Abcd1!")).thenReturn("hashed");
        doThrow(new RuntimeException("user-service down")).when(userServiceClient).createUserProfile(any());

        assertEquals("User registered successfully", authService.register(req));
    }

    @Test
    void adminCreateUserStoresHashedPasswordAndAssignedRole() {
        AdminCreateUserRequest req = new AdminCreateUserRequest();
        req.setUsername("alice");
        req.setEmail("alice@x.com");
        req.setPassword("Abcd1!");
        req.setRole("MANAGER");

        when(repository.existsByEmail("alice@x.com")).thenReturn(false);
        when(passwordEncoder.encode("Abcd1!")).thenReturn("hashed");

        AuthResponse response = authService.adminCreateUser(req);

        assertEquals("alice", response.getUsername());
        assertEquals("MANAGER", response.getRole());
        verify(repository).save(any(UserCredential.class));
    }

    @Test
    void changePasswordRequiresCorrectCurrentPassword() {
        UserCredential existing = new UserCredential();
        existing.setEmail("u@x.com");
        existing.setPassword("hashed-old");
        when(repository.findByEmail("u@x.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("Old123!", "hashed-old")).thenReturn(true);

        PasswordChangeRequest req = new PasswordChangeRequest();
        req.setCurrentPassword("wrong");
        req.setNewPassword("Abcd1!");

        assertThrows(IllegalArgumentException.class,
                () -> authService.changePassword("u@x.com", req));
    }

    @Test
    void changePasswordHashesNewPassword() {
        UserCredential existing = new UserCredential();
        existing.setEmail("u@x.com");
        existing.setPassword("hashed-old");
        when(repository.findByEmail("u@x.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("Old123!", "hashed-old")).thenReturn(true);
        when(passwordEncoder.encode("Abcd1!")).thenReturn("hashed-new");

        PasswordChangeRequest req = new PasswordChangeRequest();
        req.setCurrentPassword("Old123!");
        req.setNewPassword("Abcd1!");

        authService.changePassword("u@x.com", req);

        assertEquals("hashed-new", existing.getPassword());
        verify(repository).save(existing);
    }

    @Test
    void generateTokenResponseByEmailReturnsAuthResponse() {
        UserCredential cred = new UserCredential();
        cred.setUsername("alice");
        cred.setEmail("alice@x.com");
        cred.setRole("USER");
        when(repository.findByEmail("alice@x.com")).thenReturn(Optional.of(cred));
        when(jwtService.generateToken(cred)).thenReturn("jwt-token");

        AuthResponse response = authService.generateTokenResponseByEmail("alice@x.com");

        assertEquals("jwt-token", response.getToken());
        assertEquals("alice", response.getUsername());
        assertEquals("USER", response.getRole());
    }

    @Test
    void generateTokenResponseByEmailThrowsWhenMissing() {
        when(repository.findByEmail("missing@x.com")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> authService.generateTokenResponseByEmail("missing@x.com"));
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void notNullAuthResponse() {
        UserCredential cred = new UserCredential(1L, "u", "u@x.com", "h", "USER");
        when(repository.findByEmail("u@x.com")).thenReturn(Optional.of(cred));
        when(jwtService.generateToken(cred)).thenReturn("t");
        assertNotNull(authService.generateTokenResponseByEmail("u@x.com"));
        verify(jwtService, times(1)).generateToken(cred);
    }

    // ─── validateLoginCredentials ────────────────────────────────────────────

    @Test
    void validateLoginCredentials_throwsInvalidCredentials_whenEmailNotFound() {
        when(repository.findByEmail("nobody@x.com")).thenReturn(Optional.empty());
        com.taskflow.auth.exception.InvalidCredentialsException ex =
                assertThrows(com.taskflow.auth.exception.InvalidCredentialsException.class,
                        () -> authService.validateLoginCredentials("nobody@x.com", "Any1!pass"));
        assertEquals("Incorrect email address", ex.getMessage());
    }

    @Test
    void validateLoginCredentials_throwsInvalidCredentials_whenPasswordWrong() {
        UserCredential cred = new UserCredential(1L, "alice", "alice@x.com", "hashed", "USER");
        when(repository.findByEmail("alice@x.com")).thenReturn(Optional.of(cred));
        when(passwordEncoder.matches("wrongpass", "hashed")).thenReturn(false);
        com.taskflow.auth.exception.InvalidCredentialsException ex =
                assertThrows(com.taskflow.auth.exception.InvalidCredentialsException.class,
                        () -> authService.validateLoginCredentials("alice@x.com", "wrongpass"));
        assertEquals("Incorrect password", ex.getMessage());
    }

    @Test
    void validateLoginCredentials_passesWhenCredentialsCorrect() {
        UserCredential cred = new UserCredential(1L, "alice", "alice@x.com", "hashed", "USER");
        when(repository.findByEmail("alice@x.com")).thenReturn(Optional.of(cred));
        when(passwordEncoder.matches("Correct1!", "hashed")).thenReturn(true);
        // Should not throw
        authService.validateLoginCredentials("alice@x.com", "Correct1!");
    }
}
