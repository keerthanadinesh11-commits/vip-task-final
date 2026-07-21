package com.taskflow.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Service responsible for sending emails via JavaMailSender (Gmail SMTP).
 *
 * <p>This class is the ONLY place in the project that touches JavaMailSender.
 * All other services call this class instead of dealing with mail directly.
 *
 * <p>How it works internally:
 * <ol>
 *   <li>Spring auto-configures JavaMailSender using application.yml mail settings.</li>
 *   <li>We create a MimeMessage (supports HTML content).</li>
 *   <li>MimeMessageHelper simplifies setting To, Subject, Body fields.</li>
 *   <li>mailSender.send() opens SMTP connection to Gmail and delivers the email.</li>
 * </ol>
 *
 * <p>Security notes:
 * <ul>
 *   <li>Use Gmail App Password (not your Gmail login password).</li>
 *   <li>Enable 2FA on your Google account first, then generate App Password.</li>
 *   <li>Store credentials in environment variables, never hardcoded.</li>
 * </ul>
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    /** Spring auto-wires this from application.yml spring.mail.* configuration. */
    private final JavaMailSender mailSender;

    /** Sender address. Must match the authenticated SMTP account for Gmail/most providers. */
    @org.springframework.beans.factory.annotation.Value("${spring.mail.username:noreply@taskflow.com}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends a password-reset OTP email to the specified address.
     *
     * <p>The email contains a nicely formatted HTML body with the OTP
     * and a reminder that it expires in 5 minutes.
     *
     * @param toEmail   the recipient's email address
     * @param otp       the 6-digit OTP to include in the email
     * @throws IllegalStateException if the email cannot be sent (SMTP failure)
     */
    public void sendOtpEmail(String toEmail, String otp) {
        try {
            // Step 1: Create a MimeMessage (supports HTML, unlike SimpleMailMessage)
            MimeMessage message = mailSender.createMimeMessage();

            // Step 2: Use MimeMessageHelper to set fields easily
            // true = multipart (needed for HTML), "UTF-8" = encoding
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Step 3: Set email metadata
            helper.setFrom(fromAddress, "TaskFlow");
            helper.setTo(toEmail);
            helper.setSubject("TaskFlow — Your Password Reset OTP");

            // Step 4: Build HTML email body
            String htmlBody = buildOtpEmailBody(otp);
            helper.setText(htmlBody, true); // true = isHtml

            // Step 5: Send the email through SMTP
            mailSender.send(message);

            logger.info("[EmailService] OTP email sent successfully to: {}", toEmail);

        } catch (MessagingException | MailException | java.io.UnsupportedEncodingException ex) {
            // Log the full error for debugging, but throw a clean user-facing error
            logger.error("[EmailService] Failed to send OTP email to {}: {}", toEmail, ex.getMessage());
            throw new IllegalStateException("Failed to send OTP email. Please try again later.", ex);
        }
    }

    /**
     * Sends a registration welcome email after account creation.
     *
     * @param toEmail  the new user's email address
     * @param username the new user's chosen username
     */
    public void sendWelcomeEmail(String toEmail, String username) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, "TaskFlow");
            helper.setTo(toEmail);
            helper.setSubject("Welcome to TaskFlow!");
            helper.setText(buildWelcomeEmailBody(username), true);

            mailSender.send(message);
            logger.info("[EmailService] Welcome email sent to: {}", toEmail);

        } catch (MessagingException | MailException | java.io.UnsupportedEncodingException ex) {
            // Welcome email failure is non-critical — log but don't block registration
            logger.warn("[EmailService] Could not send welcome email to {}: {}", toEmail, ex.getMessage());
        }
    }

    // ── Private HTML Template Builders ───────────────────────────────────────

    /**
     * Builds the HTML body for the OTP email.
     * Professional-looking template with the OTP displayed prominently.
     */
    private String buildOtpEmailBody(String otp) {
        return "<!DOCTYPE html>"
             + "<html><body style='font-family: Arial, sans-serif; background:#f4f4f4; padding:20px;'>"
             + "<div style='max-width:500px; margin:auto; background:#ffffff; border-radius:8px;"
             + "     padding:30px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);'>"
             + "  <h2 style='color:#2c3e50; margin-bottom:10px;'>🔐 Password Reset OTP</h2>"
             + "  <p style='color:#555;'>You requested a password reset for your <strong>TaskFlow</strong> account.</p>"
             + "  <p style='color:#555;'>Use the OTP below to reset your password:</p>"
             + "  <div style='text-align:center; margin:25px 0;'>"
             + "    <span style='font-size:36px; font-weight:bold; letter-spacing:10px;"
             + "          color:#2980b9; background:#eaf4fb; padding:15px 30px;"
             + "          border-radius:8px; display:inline-block;'>" + otp + "</span>"
             + "  </div>"
             + "  <p style='color:#e74c3c; font-weight:bold;'>⏰ This OTP expires in <u>5 minutes</u>.</p>"
             + "  <p style='color:#888; font-size:13px;'>"
             + "    If you did not request a password reset, please ignore this email.<br>"
             + "    Your account remains secure."
             + "  </p>"
             + "  <hr style='border:none; border-top:1px solid #eee; margin:20px 0;'/>"
             + "  <p style='color:#aaa; font-size:12px; text-align:center;'>TaskFlow — Task Management System</p>"
             + "</div></body></html>";
    }

    /**
     * Builds the HTML body for the welcome email sent after registration.
     */
    private String buildWelcomeEmailBody(String username) {
        return "<!DOCTYPE html>"
             + "<html><body style='font-family: Arial, sans-serif; background:#f4f4f4; padding:20px;'>"
             + "<div style='max-width:500px; margin:auto; background:#ffffff; border-radius:8px;"
             + "     padding:30px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);'>"
             + "  <h2 style='color:#27ae60;'>👋 Welcome to TaskFlow, " + username + "!</h2>"
             + "  <p style='color:#555;'>Your account has been created successfully.</p>"
             + "  <p style='color:#555;'>You can now log in and start managing your tasks.</p>"
             + "  <hr style='border:none; border-top:1px solid #eee; margin:20px 0;'/>"
             + "  <p style='color:#aaa; font-size:12px; text-align:center;'>TaskFlow — Task Management System</p>"
             + "</div></body></html>";
    }
}
