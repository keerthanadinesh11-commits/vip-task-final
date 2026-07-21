package com.taskflow.auth.dto;

/**
 * Lightweight view of a manager account, used by the super-admin approval
 * queue and by the public "approved managers by department" lookup that the
 * user-registration screen consumes.
 */
public class ManagerSummary {

    private Long id;
    private String username;
    private String email;
    private String department;
    private String status;

    public ManagerSummary() {}

    public ManagerSummary(Long id, String username, String email, String department, String status) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.department = department;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
