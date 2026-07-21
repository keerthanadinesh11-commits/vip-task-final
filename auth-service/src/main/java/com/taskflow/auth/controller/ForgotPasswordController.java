package com.taskflow.auth.controller;

import com.taskflow.auth.dto.ForgotPasswordRequest;
import com.taskflow.auth.dto.ResetPasswordRequest;
import com.taskflow.auth.dto.SendOtpRequest;
import com.taskflow.auth.dto.VerifyOtpRequest;
import com.taskflow.auth.service.ForgotPasswordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller exposing the OTP-based password reset endpoints.
 *
 * <p>All endpoints are public (no JWT required) — configured in SecurityConfig.
 *
 * <p>API Flow:
 * <pre>
 *   POST /auth/send-otp         → send OTP to email (alias, recommended)
 *   POST /auth/forgot-password  → send OTP to email (original endpoint, kept for compatibility)
 *   POST /auth/verify-otp       → verify the OTP
 *   POST /auth/reset-password   → reset password using verified OTP
 * </pre>
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Forgot Password", description = "OTP-based password reset flow using JavaMailSender")
public class ForgotPasswordController {

    /** Sonar: avoid duplicate string literals. */
    private static final String MESSAGE_KEY = "message";

    private final ForgotPasswordService forgotPasswordService;

    public ForgotPasswordController(ForgotPasswordService forgotPasswordService) {
        this.forgotPasswordService = forgotPasswordService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /auth/send-otp  (new recommended endpoint)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sends a 6-digit OTP to the user's registered email address.
     *
     * <p>OTP is generated using SecureRandom, stored in the database with
     * a 5-minute expiry, and delivered via Gmail SMTP using JavaMailSender.
     */
    @PostMapping("/send-otp")
    @Operation(
            summary = "Send OTP to email",
            description = "Generates a 6-digit OTP, stores it in DB with 5-min expiry, and emails it via Gmail SMTP."
    )
    @ApiResponse(responseCode = "200", description = "OTP sent to registered email")
    @ApiResponse(responseCode = "400", description = "Email not registered or invalid format")
    public ResponseEntity<Map<String, String>> sendOtp(
            @Valid @RequestBody SendOtpRequest request) {

        forgotPasswordService.generateOtp(request.getEmail());

        return ResponseEntity.ok(Map.of(
                MESSAGE_KEY,
                "OTP has been sent to your registered email address. Valid for 5 minutes."
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /auth/forgot-password  (original endpoint — kept for compatibility)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Original endpoint — same behaviour as /send-otp.
     * Kept so existing frontend calls continue to work unchanged.
     */
    @PostMapping("/forgot-password")
    @Operation(
            summary = "Request a password-reset OTP (legacy endpoint)",
            description = "Same as /send-otp. Kept for backward compatibility with existing frontend."
    )
    @ApiResponse(responseCode = "200", description = "OTP sent to registered email")
    @ApiResponse(responseCode = "400", description = "Email not registered or invalid format")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        forgotPasswordService.generateOtp(request.getEmail());

        return ResponseEntity.ok(Map.of(
                MESSAGE_KEY,
                "OTP has been sent to your registered email address. Valid for 5 minutes."
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /auth/verify-otp
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifies the 6-digit OTP submitted by the user.
     *
     * <p>Checks: OTP exists, not expired, matches stored value.
     * Marks OTP as verified so /reset-password can proceed.
     */
    @PostMapping("/verify-otp")
    @Operation(
            summary = "Verify the OTP",
            description = "Validates OTP against DB. Marks it verified so reset-password can proceed."
    )
    @ApiResponse(responseCode = "200", description = "OTP verified successfully")
    @ApiResponse(responseCode = "400", description = "Invalid or expired OTP")
    public ResponseEntity<Map<String, String>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        forgotPasswordService.verifyOtp(request.getEmail(), request.getOtp());

        return ResponseEntity.ok(Map.of(
                MESSAGE_KEY,
                "OTP verified successfully"
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /auth/reset-password
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resets the user's password after successful OTP verification.
     *
     * <p>Requires the OTP to have been verified via /verify-otp first.
     * Password is BCrypt-encoded before saving. OTP record deleted after use.
     */
    @PostMapping("/reset-password")
    @Operation(
            summary = "Reset password using verified OTP",
            description = "Requires prior OTP verification. BCrypt-encodes and saves the new password."
    )
    @ApiResponse(responseCode = "200", description = "Password reset successfully")
    @ApiResponse(responseCode = "400", description = "OTP not verified, expired, or weak password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        forgotPasswordService.resetPassword(
                request.getEmail(),
                request.getOtp(),
                request.getNewPassword()
        );

        return ResponseEntity.ok(Map.of(
                MESSAGE_KEY,
                "Password reset successfully"
        ));
    }
}
