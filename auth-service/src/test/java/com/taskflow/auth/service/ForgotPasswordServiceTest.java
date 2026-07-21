package com.taskflow.auth.service;

import com.taskflow.auth.entity.OtpDetail;
import com.taskflow.auth.entity.UserCredential;
import com.taskflow.auth.repository.UserCredentialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ForgotPasswordService.
 * All dependencies (OtpService, EmailService, Repository) are mocked.
 */
@ExtendWith(MockitoExtension.class)
class ForgotPasswordServiceTest {

    @Mock private UserCredentialRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private OtpService otpService;
    @Mock private EmailService emailService;

    @InjectMocks
    private ForgotPasswordService forgotPasswordService;

    private static final String EMAIL = "alice@test.com";
    private static final String OTP   = "123456";
    private static final String NEW_PW = "NewPass1!";

    // ── generateOtp ───────────────────────────────────────────────────────────

    @Test
    void generateOtp_success_generatesAndEmails() {
        // Arrange
        UserCredential user = new UserCredential(1L, "alice", EMAIL, "hashed", "USER");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(otpService.generateAndSaveOtp(EMAIL)).thenReturn(OTP);

        // Act
        forgotPasswordService.generateOtp(EMAIL);

        // Assert: OTP generated and emailed
        verify(otpService).generateAndSaveOtp(EMAIL);
        verify(emailService).sendOtpEmail(EMAIL, OTP);
    }

    @Test
    void generateOtp_unknownEmail_throwsException() {
        // Arrange: no user found
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> forgotPasswordService.generateOtp(EMAIL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No account found");

        // OtpService and EmailService should NOT be called
        verifyNoInteractions(otpService, emailService);
    }

    // ── verifyOtp ─────────────────────────────────────────────────────────────

    @Test
    void verifyOtp_success_delegatesToOtpService() {
        // Act
        forgotPasswordService.verifyOtp(EMAIL, OTP);

        // Assert
        verify(otpService).verifyOtp(EMAIL, OTP);
    }

    @Test
    void verifyOtp_invalidOtp_propagatesException() {
        // Arrange
        doThrow(new IllegalArgumentException("Invalid OTP"))
                .when(otpService).verifyOtp(EMAIL, OTP);

        // Act & Assert
        assertThatThrownBy(() -> forgotPasswordService.verifyOtp(EMAIL, OTP))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid OTP");
    }

    // ── resetPassword ─────────────────────────────────────────────────────────

    @Test
    void resetPassword_success_encodesAndSaves() {
        // Arrange
        UserCredential user = new UserCredential(1L, "alice", EMAIL, "oldHash", "USER");
        when(otpService.isOtpVerified(EMAIL, OTP)).thenReturn(true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(NEW_PW)).thenReturn("newHash");

        // Act
        forgotPasswordService.resetPassword(EMAIL, OTP, NEW_PW);

        // Assert: password updated with BCrypt and OTP deleted
        verify(passwordEncoder).encode(NEW_PW);
        verify(userRepository).save(user);
        verify(otpService).deleteOtp(EMAIL);
    }

    @Test
    void resetPassword_otpNotVerified_throwsException() {
        // Arrange: OTP has not been verified
        when(otpService.isOtpVerified(EMAIL, OTP)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> forgotPasswordService.resetPassword(EMAIL, OTP, NEW_PW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OTP not verified");

        // Password should NOT be changed
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void resetPassword_weakPassword_throwsException() {
        // Arrange: OTP verified but password is too weak
        when(otpService.isOtpVerified(EMAIL, OTP)).thenReturn(true);

        // Act & Assert: "weak" doesn't meet PasswordPolicy
        assertThatThrownBy(() -> forgotPasswordService.resetPassword(EMAIL, OTP, "weak"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password must be at least 6 characters");

        // User record should NOT be touched
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_unknownEmail_throwsException() {
        // Arrange: OTP verified but user deleted meanwhile
        when(otpService.isOtpVerified(EMAIL, OTP)).thenReturn(true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> forgotPasswordService.resetPassword(EMAIL, OTP, NEW_PW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No account found");
    }
}
