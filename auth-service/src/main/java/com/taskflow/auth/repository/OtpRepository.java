package com.taskflow.auth.repository;

import com.taskflow.auth.entity.OtpDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repository for OTP database operations.
 *
 * <p>Spring Data JPA automatically implements all these methods at runtime.
 * No manual SQL needed — just declare the method signature.
 */
public interface OtpRepository extends JpaRepository<OtpDetail, Long> {

    /**
     * Find the most recent OTP record for an email address.
     * Used during verification to check if the submitted OTP matches.
     *
     * @param email the user's email (case-sensitive)
     * @return the OTP record if it exists
     */
    Optional<OtpDetail> findByEmail(String email);

    /**
     * Delete all OTP records for a given email.
     * Called BEFORE generating a new OTP (prevents duplicates)
     * and AFTER successful password reset (cleanup).
     *
     * @param email the user's email
     */
    @Modifying
    @Query("DELETE FROM OtpDetail o WHERE o.email = :email")
    void deleteByEmail(@Param("email") String email);
}
