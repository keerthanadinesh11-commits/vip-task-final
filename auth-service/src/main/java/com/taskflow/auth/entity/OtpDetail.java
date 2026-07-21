package com.taskflow.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Database entity that stores OTP details for password reset and email verification.
 *
 * <p>Table: otp_details
 * <p>Each row holds a 6-digit OTP for one email address.
 * Old OTPs are deleted before a new one is generated (see OtpService).
 */
@Entity
@Table(name = "otp_details")
public class OtpDetail {

    /** Primary key - auto-incremented by the database. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The email address this OTP belongs to. */
    @Column(nullable = false, length = 100)
    private String email;

    /** The 6-digit OTP code (stored as plain text - short-lived, not sensitive long-term). */
    @Column(nullable = false, length = 6)
    private String otp;

    /** The exact time when this OTP expires (generated time + 5 minutes). */
    @Column(nullable = false)
    private LocalDateTime expiryTime;

    /**
     * Whether this OTP has been successfully verified.
     * Set to true after the user calls /verify-otp successfully.
     * Used to allow /reset-password without re-verifying.
     */
    @Column(nullable = false)
    private boolean verified;

    /** No-arg constructor required by JPA. */
    public OtpDetail() {
        // JPA requires a no-arg constructor
    }

    /**
     * Convenience constructor to create a new OTP record.
     *
     * @param email      the user's email
     * @param otp        the 6-digit OTP
     * @param expiryTime when this OTP expires
     */
    public OtpDetail(String email, String otp, LocalDateTime expiryTime) {
        this.email = email;
        this.otp = otp;
        this.expiryTime = expiryTime;
        this.verified = false; // always starts unverified
    }

    // ── Getters and Setters ───────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }

    public LocalDateTime getExpiryTime() { return expiryTime; }
    public void setExpiryTime(LocalDateTime expiryTime) { this.expiryTime = expiryTime; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
}
