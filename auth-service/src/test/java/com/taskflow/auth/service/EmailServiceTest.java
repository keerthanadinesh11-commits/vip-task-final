package com.taskflow.auth.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailService.
 * JavaMailSender is mocked — no real SMTP connection needed.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    private static final String TO_EMAIL = "alice@test.com";
    private static final String OTP = "123456";
    private static final String USERNAME = "alice";

    @BeforeEach
    void setUp() {
        // MimeMessage creation always returns our mock
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    // ── sendOtpEmail ──────────────────────────────────────────────────────────

    @Test
    void sendOtpEmail_success_callsMailSenderSend() {
        // Act
        emailService.sendOtpEmail(TO_EMAIL, OTP);

        // Assert: mailSender.send() was called once with our message
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendOtpEmail_createsNewMimeMessage() {
        // Act
        emailService.sendOtpEmail(TO_EMAIL, OTP);

        // Assert: a MimeMessage was created
        verify(mailSender).createMimeMessage();
    }

    @Test
    void sendOtpEmail_mailException_throwsIllegalStateException() {
        // Arrange: SMTP fails
        doThrow(new MailSendException("SMTP connection failed"))
                .when(mailSender).send(any(MimeMessage.class));

        // Act & Assert: wraps MailException in IllegalStateException
        assertThatThrownBy(() -> emailService.sendOtpEmail(TO_EMAIL, OTP))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to send OTP email");
    }

    // ── sendWelcomeEmail ──────────────────────────────────────────────────────

    @Test
    void sendWelcomeEmail_success_callsMailSenderSend() {
        // Act
        emailService.sendWelcomeEmail(TO_EMAIL, USERNAME);

        // Assert
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendWelcomeEmail_mailException_doesNotThrow() {
        // Arrange: SMTP fails — welcome email is non-critical
        doThrow(new MailSendException("SMTP failed"))
                .when(mailSender).send(any(MimeMessage.class));

        // Act & Assert: should NOT throw — just logs warning
        emailService.sendWelcomeEmail(TO_EMAIL, USERNAME);
        // No exception = test passes
    }
}
