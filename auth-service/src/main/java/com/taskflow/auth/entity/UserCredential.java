package com.taskflow.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Stored credentials for a TaskFlow user. Contains the BCrypt-encoded
 * password and the role used to authorize requests across services.
 *
 * Role hierarchy:
 *   SUPER_ADMIN -> manages MANAGERs (approves/rejects their registration)
 *   MANAGER     -> belongs to a department; manages USERs in that department.
 *                  Cannot log in until status = APPROVED.
 *   USER        -> belongs to a department (auto-assigned to that department's manager).
 */
@Entity
@Table(name = "user_credential")
public class UserCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 20)
    private String role;

    /** Approval status. USER/SUPER_ADMIN are always APPROVED; MANAGER starts PENDING. */
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'APPROVED'")
    private String status = "APPROVED";

    /** Department this account belongs to (e.g. SALES, BUSINESS_UNIT). Null for SUPER_ADMIN. */
    @Column(length = 50)
    private String department;

    /** For USERs: the email of the manager they report to. Null for other roles. */
    @Column(name = "manager_email", length = 100)
    private String managerEmail;

    public UserCredential() {
        // JPA needs a no-arg constructor
    }

    public UserCredential(Long id, String username, String email, String password, String role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getManagerEmail() { return managerEmail; }
    public void setManagerEmail(String managerEmail) { this.managerEmail = managerEmail; }
}
