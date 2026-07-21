package com.taskflow.auth.service;

import com.taskflow.auth.entity.OtpDetail;
import com.taskflow.auth.repository.OtpRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Service that handles OTP lifecycle: generate → store → verify → invalidate.
 *
 * <p>This replaces the old in-memory {@code OtpStore} with a proper
 * database-backed solution using the {@code otp_details} table.
 *
 * <p>OTP flow:
 * <pre>
 *  User requests OTP
 *       ↓
 *  Delete any existing OTP for this email (resend support)
 *       ↓
 *  Generate random 6-digit OTP using SecureRandom
 *       ↓
 *  Save OTP to database with expiryTime = now + 5 minutes
 *       ↓
 *  EmailService sends OTP to user's email
 *       ↓
 *  User submits OTP → verify: check exists, not expired, matches
 *       ↓
 *  Mark OTP as verified (allows password reset)
 *       ↓
 *  After password reset → delete OTP record (cleanup)
 * </pre>
 *
 * <p>Security best practices applied:
 * <ul>
 *   <li>SecureRandom (not Math.random) for cryptographic safety</li>
 *   <li>OTP expires after exactly {@value #OTP_VALIDITY_MINUTES} minutes</li>
 *   <li>Old OTPs deleted before new ones are created</li>
 *   <li>OTP invalidated immediately after successful password reset</li>
 * </ul>
 */
@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);

    /** OTP expires after 5 minutes — industry standard for security. */
    public static final int OTP_VALIDITY_MINUTES = 5;

    /**
     * SecureRandom is cryptographically secure, unlike java.util.Random.
     * It uses OS-level entropy sources (e.g., /dev/urandom on Linux).
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final OtpRepository otpRepository;

    public OtpService(OtpRepository otpRepository) {
        this.otpRepository = otpRepository;
    }

    /**
     * Generates a new OTP for the given email and saves it to the database.
     *
     * <p>Any existing OTP for this email is deleted first (supports resend).
     *
     * @param email the user's email address
     * @return the generated 6-digit OTP string (to be sent via email)
     */
    @Transactional
    public String generateAndSaveOtp(String email) {
        // Step 1: Delete any existing OTP for this email (supports resend)
        otpRepository.deleteByEmail(email.toLowerCase());

        // Step 2: Generate a cryptographically secure 6-digit OTP
        // nextInt(900_000) gives 0–899999, adding 100_000 gives 100000–999999
        String otp = String.valueOf(SECURE_RANDOM.nextInt(900_000) + 100_000);

        // Step 3: Set expiry time = current time + 5 minutes
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES);

        // Step 4: Save to database
        OtpDetail otpDetail = new OtpDetail(email.toLowerCase(), otp, expiryTime);
        otpRepository.save(otpDetail);

        logger.info("[OtpService] OTP generated for email: {} | Expires at: {}", email, expiryTime);

        return otp;
    }

    /**
     * Verifies the OTP submitted by the user.
     *
     * <p>Checks:
     * <ol>
     *   <li>OTP record exists for this email</li>
     *   <li>OTP has not expired (expiryTime is in the future)</li>
     *   <li>Submitted OTP matches stored OTP exactly</li>
     * </ol>
     *
     * <p>On success, marks the OTP as verified so /reset-password can proceed.
     *
     * @param email the user's email
     * @param otp   the 6-digit OTP submitted by the user
     * @throws IllegalArgumentException if OTP is invalid or expired
     */
    @Transactional
    public void verifyOtp(String email, String otp) {
        // Step 1: Look up OTP record in database
        OtpDetail otpDetail = otpRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No OTP found for this email. Please request a new OTP."));

        // Step 2: Check if OTP has expired
        if (LocalDateTime.now().isAfter(otpDetail.getExpiryTime())) {
            // Clean up expired OTP from database
            otpRepository.deleteByEmail(email.toLowerCase());
            throw new IllegalArgumentException(
                    "OTP has expired. Please request a new OTP.");
        }

        // Step 3: Check if submitted OTP matches stored OTP
        if (!otpDetail.getOtp().equals(otp)) {
            throw new IllegalArgumentException("Invalid OTP. Please check and try again.");
        }

        // Step 4: Mark OTP as verified (so /reset-password knows verification passed)
        otpDetail.setVerified(true);
        otpRepository.save(otpDetail);

        logger.info("[OtpService] OTP verified successfully for email: {}", email);
    }

    /**
     * Checks whether the OTP for the given email has been verified.
     *
     * <p>Called by ForgotPasswordService before allowing password reset.
     *
     * @param email the user's email
     * @param otp   the OTP to validate
     * @return true if OTP exists, is not expired, and is verified
     */
    public boolean isOtpVerified(String email, String otp) {
        return otpRepository.findByEmail(email.toLowerCase())
                .map(detail -> {
                    // Must not be expired AND must be verified AND must match
                    boolean notExpired = LocalDateTime.now().isBefore(detail.getExpiryTime());
                    boolean matches = detail.getOtp().equals(otp);
                    return notExpired && matches && detail.isVerified();
                })
                .orElse(false);
    }

    /**
     * Deletes the OTP record for the given email.
     *
     * <p>Called after a successful password reset to clean up the database.
     *
     * @param email the user's email
     */
    @Transactional
    public void deleteOtp(String email) {
        otpRepository.deleteByEmail(email.toLowerCase());
        logger.info("[OtpService] OTP record deleted for email: {}", email);
    }
}
