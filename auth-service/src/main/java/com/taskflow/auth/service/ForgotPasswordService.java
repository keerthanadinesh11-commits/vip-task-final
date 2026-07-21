package com.taskflow.auth.service;

import com.taskflow.auth.entity.UserCredential;
import com.taskflow.auth.repository.UserCredentialRepository;
import com.taskflow.auth.util.PasswordPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the full forgot-password / OTP / reset-password flow.
 *
 * <p>Flow:
 * <pre>
 *   POST /auth/forgot-password  →  generateOtp()   → OtpService + EmailService
 *   POST /auth/verify-otp       →  verifyOtp()     → OtpService
 *   POST /auth/reset-password   →  resetPassword() → OtpService + UserCredentialRepository
 * </pre>
 *
 * <p>This service is deliberately thin — it delegates OTP logic to {@link OtpService}
 * and email sending to {@link EmailService}, keeping each class focused on one job.
 */
@Service
public class ForgotPasswordService {

    private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordService.class);

    private final UserCredentialRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;

    public ForgotPasswordService(UserCredentialRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 OtpService otpService,
                                 EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.emailService = emailService;
    }

    /**
     * Step 1 of password reset flow: generates a 6-digit OTP and emails it.
     *
     * <p>Validates that the email belongs to an existing account before
     * generating an OTP — prevents OTP spam to arbitrary addresses.
     *
     * @param email the user's registered email address
     * @throws IllegalArgumentException if no account exists for this email
     */
    public void generateOtp(String email) {
        // Guard: only generate OTP for real accounts
        userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No account found for this email address"));

        // Generate OTP and persist it to the database (5-minute expiry)
        String otp = otpService.generateAndSaveOtp(email);

        // Send OTP to user's email via JavaMailSender
        emailService.sendOtpEmail(email, otp);

        logger.info("[ForgotPasswordService] OTP generated and emailed to: {}", email);
    }

    /**
     * Step 2 of password reset flow: verifies the OTP submitted by the user.
     *
     * <p>Delegates all verification logic (expiry check, match check) to OtpService.
     *
     * @param email the user's email
     * @param otp   the 6-digit OTP from the email
     * @throws IllegalArgumentException if OTP is invalid or expired
     */
    public void verifyOtp(String email, String otp) {
        // OtpService handles: exists? expired? matches? marks verified.
        otpService.verifyOtp(email, otp);
    }

    /**
     * Step 3 of password reset flow: resets the password after OTP verification.
     *
     * <p>Requires the OTP to have been verified in Step 2. After a successful
     * reset the OTP record is deleted from the database.
     *
     * @param email       the user's email
     * @param otp         the same OTP used in Step 2 (used to confirm it was verified)
     * @param newPassword the desired new password (must pass PasswordPolicy)
     * @throws IllegalArgumentException if OTP not verified, expired, or password is weak
     */
    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        // Guard: OTP must be verified before resetting password
        if (!otpService.isOtpVerified(email, otp)) {
            throw new IllegalArgumentException(
                    "OTP not verified or has expired. Please verify your OTP first.");
        }

        // Guard: new password must meet the platform password policy
        if (!PasswordPolicy.isValid(newPassword)) {
            throw new IllegalArgumentException(PasswordPolicy.MESSAGE);
        }

        // Find and update the user's password
        UserCredential credential = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No account found for this email address"));

        // BCrypt-encode and persist the new password
        credential.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(credential);

        // Clean up: delete the OTP record — it cannot be reused
        otpService.deleteOtp(email);

        logger.info("[ForgotPasswordService] Password reset successfully for: {}", email);
    }
}
