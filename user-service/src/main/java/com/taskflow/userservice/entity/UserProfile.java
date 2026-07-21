package com.taskflow.userservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_profile")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 50)
    private String username;

    @Column(unique = true, length = 100)
    private String email;

    @Column(length = 20)
    private String role;

    /** Approval status: PENDING / APPROVED / REJECTED. */
    @Column(length = 20)
    private String status = "APPROVED";

    /** Department this account belongs to (e.g. SALES, BUSINESS_UNIT). */
    @Column(length = 50)
    private String department;

    /** For USERs: email of the manager they report to. */
    @Column(name = "manager_email", length = 100)
    private String managerEmail;

    @Column(length = 500)
    private String bio;

    @Column(name = "profile_image", length = 500)
    private String profileImage;

    @Column(name = "otp", length = 10)
    private String otp;

    @Column(name = "otp_expiry")
    private LocalDateTime otpExpiry;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // activity log stored as JSON string (simple approach)
    @Column(name = "activity_log", columnDefinition = "TEXT")
    private String activityLog;

    public UserProfile() {}

    public UserProfile(Long id, String username, String email, String role) {
        this.id = id; this.username = username;
        this.email = email; this.role = role;
    }

    public Long getId()                            { return id; }
    public void setId(Long id)                     { this.id = id; }
    public String getUsername()                    { return username; }
    public void setUsername(String u)              { this.username = u; }
    public String getEmail()                       { return email; }
    public void setEmail(String e)                 { this.email = e; }
    public String getRole()                        { return role; }
    public void setRole(String r)                  { this.role = r; }
    public String getStatus()                      { return status; }
    public void setStatus(String s)                { this.status = s; }
    public String getDepartment()                  { return department; }
    public void setDepartment(String d)            { this.department = d; }
    public String getManagerEmail()                { return managerEmail; }
    public void setManagerEmail(String m)          { this.managerEmail = m; }
    public String getBio()                         { return bio; }
    public void setBio(String b)                   { this.bio = b; }
    public String getProfileImage()                { return profileImage; }
    public void setProfileImage(String p)          { this.profileImage = p; }
    public String getOtp()                         { return otp; }
    public void setOtp(String o)                   { this.otp = o; }
    public LocalDateTime getOtpExpiry()            { return otpExpiry; }
    public void setOtpExpiry(LocalDateTime t)      { this.otpExpiry = t; }
    public LocalDateTime getLastLogin()            { return lastLogin; }
    public void setLastLogin(LocalDateTime t)      { this.lastLogin = t; }
    public LocalDateTime getCreatedAt()            { return createdAt; }
    public void setCreatedAt(LocalDateTime t)      { this.createdAt = t; }
    public String getActivityLog()                 { return activityLog; }
    public void setActivityLog(String a)           { this.activityLog = a; }
}
