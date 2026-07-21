package com.taskflow.auth.service;

import com.taskflow.auth.entity.OtpDetail;
import com.taskflow.auth.repository.OtpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OtpService.
 * All database calls are mocked — no real DB needed.
 */
@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpRepository otpRepository;

    @InjectMocks
    private OtpService otpService;

    private static final String EMAIL = "alice@test.com";
    private static final String OTP = "123456";

    // ── generateAndSaveOtp ────────────────────────────────────────────────────

    @Test
    void generateAndSaveOtp_deletesOldOtpFirst() {
        // Arrange
        when(otpRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        otpService.generateAndSaveOtp(EMAIL);

        // Assert: old OTP deleted before new one saved
        verify(otpRepository).deleteByEmail(EMAIL.toLowerCase());
        verify(otpRepository).save(any(OtpDetail.class));
    }

    @Test
    void generateAndSaveOtp_returns6DigitOtp() {
        // Arrange
        when(otpRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        String otp = otpService.generateAndSaveOtp(EMAIL);

        // Assert: OTP is exactly 6 digits
        assertThat(otp).matches("\\d{6}");
    }

    @Test
    void generateAndSaveOtp_savesCorrectExpiryTime() {
        // Arrange
        ArgumentCaptor<OtpDetail> captor = ArgumentCaptor.forClass(OtpDetail.class);
        when(otpRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        LocalDateTime before = LocalDateTime.now();

        // Act
        otpService.generateAndSaveOtp(EMAIL);

        // Assert: expiry is within expected 5-minute window
        verify(otpRepository).save(captor.capture());
        OtpDetail saved = captor.getValue();
        assertThat(saved.getExpiryTime())
                .isAfterOrEqualTo(before.plusMinutes(OtpService.OTP_VALIDITY_MINUTES - 1))
                .isBeforeOrEqualTo(LocalDateTime.now().plusMinutes(OtpService.OTP_VALIDITY_MINUTES + 1));
    }

    @Test
    void generateAndSaveOtp_normalizesEmailToLowercase() {
        // Arrange
        when(otpRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        ArgumentCaptor<OtpDetail> captor = ArgumentCaptor.forClass(OtpDetail.class);

        // Act
        otpService.generateAndSaveOtp("ALICE@TEST.COM");

        // Assert
        verify(otpRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("alice@test.com");
    }

    // ── verifyOtp ─────────────────────────────────────────────────────────────

    @Test
    void verifyOtp_success_marksVerified() {
        // Arrange: valid, non-expired OTP in DB
        OtpDetail detail = new OtpDetail(EMAIL, OTP, LocalDateTime.now().plusMinutes(4));
        when(otpRepository.findByEmail(EMAIL)).thenReturn(Optional.of(detail));
        when(otpRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        otpService.verifyOtp(EMAIL, OTP);

        // Assert: OTP was marked verified
        assertThat(detail.isVerified()).isTrue();
        verify(otpRepository).save(detail);
    }

    @Test
    void verifyOtp_noRecord_throwsException() {
        // Arrange: no OTP in DB
        when(otpRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> otpService.verifyOtp(EMAIL, OTP))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No OTP found");
    }

    @Test
    void verifyOtp_expiredOtp_throwsException() {
        // Arrange: OTP that expired 1 minute ago
        OtpDetail detail = new OtpDetail(EMAIL, OTP, LocalDateTime.now().minusMinutes(1));
        when(otpRepository.findByEmail(EMAIL)).thenReturn(Optional.of(detail));

        // Act & Assert
        assertThatThrownBy(() -> otpService.verifyOtp(EMAIL, OTP))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired");

        // Expired OTP should be deleted
        verify(otpRepository).deleteByEmail(EMAIL.toLowerCase());
    }

    @Test
    void verifyOtp_wrongOtp_throwsException() {
        // Arrange: correct OTP is "123456" but user submits "999999"
        OtpDetail detail = new OtpDetail(EMAIL, OTP, LocalDateTime.now().plusMinutes(4));
        when(otpRepository.findByEmail(EMAIL)).thenReturn(Optional.of(detail));

        // Act & Assert
        assertThatThrownBy(() -> otpService.verifyOtp(EMAIL, "999999"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid OTP");
    }

    // ── isOtpVerified ─────────────────────────────────────────────────────────

    @Test
    void isOtpVerified_returnsTrueWhenVerifiedAndNotExpired() {
        // Arrange
        OtpDetail detail = new OtpDetail(EMAIL, OTP, LocalDateTime.now().plusMinutes(4));
        detail.setVerified(true);
        when(otpRepository.findByEmail(EMAIL)).thenReturn(Optional.of(detail));

        // Act & Assert
        assertThat(otpService.isOtpVerified(EMAIL, OTP)).isTrue();
    }

    @Test
    void isOtpVerified_returnsFalseWhenNotVerified() {
        // Arrange: OTP exists but not yet verified
        OtpDetail detail = new OtpDetail(EMAIL, OTP, LocalDateTime.now().plusMinutes(4));
        detail.setVerified(false);
        when(otpRepository.findByEmail(EMAIL)).thenReturn(Optional.of(detail));

        // Act & Assert
        assertThat(otpService.isOtpVerified(EMAIL, OTP)).isFalse();
    }

    @Test
    void isOtpVerified_returnsFalseWhenExpired() {
        // Arrange: verified but expired
        OtpDetail detail = new OtpDetail(EMAIL, OTP, LocalDateTime.now().minusMinutes(1));
        detail.setVerified(true);
        when(otpRepository.findByEmail(EMAIL)).thenReturn(Optional.of(detail));

        // Act & Assert
        assertThat(otpService.isOtpVerified(EMAIL, OTP)).isFalse();
    }

    @Test
    void isOtpVerified_returnsFalseWhenNoRecord() {
        when(otpRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        assertThat(otpService.isOtpVerified(EMAIL, OTP)).isFalse();
    }

    @Test
    void isOtpVerified_returnsFalseWhenOtpMismatch() {
        // Arrange: verified, not expired, but OTP doesn't match
        OtpDetail detail = new OtpDetail(EMAIL, OTP, LocalDateTime.now().plusMinutes(4));
        detail.setVerified(true);
        when(otpRepository.findByEmail(EMAIL)).thenReturn(Optional.of(detail));

        // Act & Assert
        assertThat(otpService.isOtpVerified(EMAIL, "000000")).isFalse();
    }

    // ── deleteOtp ─────────────────────────────────────────────────────────────

    @Test
    void deleteOtp_callsRepositoryDeleteByEmail() {
        otpService.deleteOtp(EMAIL);
        verify(otpRepository).deleteByEmail(EMAIL.toLowerCase());
    }
}
