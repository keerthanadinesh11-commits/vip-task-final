package com.taskflow.auth.service;

import org.springframework.stereotype.Component;

/**
 * Legacy in-memory OTP store — retained only for backward compatibility.
 * All logic has moved to {@link OtpService}.
 *
 * @deprecated Use {@link OtpService} instead.
 */
@Deprecated
@Component
public class OtpStore {

    public static final int OTP_VALIDITY_MINUTES = 5;

    /** No-op: OtpService handles saving. */
    public void save(String email, String otp) { // NOSONAR — intentional no-op
        // Delegated to OtpService
    }

    /** Always returns false: OtpService handles verification. */
    public boolean verify(String email, String otp) {
        return false;
    }

    /** No-op: OtpService handles invalidation. */
    public void invalidate(String email) { // NOSONAR — intentional no-op
        // Delegated to OtpService
    }
}
