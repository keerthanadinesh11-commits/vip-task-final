package com.taskflow.userservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileDto {

    private Long id;
    private String username;
    private String email;
    private String role;
    private String status;
    private String department;
    private String managerEmail;
    private String bio;
    private String profileImage;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private String activityLog;

    public UserProfileDto() {}

    public UserProfileDto(Long id, String username, String email, String role) {
        this.id = id; this.username = username;
        this.email = email; this.role = role;
    }

    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }
    public String getUsername()                { return username; }
    public void setUsername(String u)          { this.username = u; }
    public String getEmail()                   { return email; }
    public void setEmail(String e)             { this.email = e; }
    public String getRole()                    { return role; }
    public void setRole(String r)              { this.role = r; }
    public String getStatus()                  { return status; }
    public void setStatus(String s)            { this.status = s; }
    public String getDepartment()              { return department; }
    public void setDepartment(String d)        { this.department = d; }
    public String getManagerEmail()            { return managerEmail; }
    public void setManagerEmail(String m)      { this.managerEmail = m; }
    public String getBio()                     { return bio; }
    public void setBio(String b)               { this.bio = b; }
    public String getProfileImage()            { return profileImage; }
    public void setProfileImage(String p)      { this.profileImage = p; }
    public LocalDateTime getLastLogin()        { return lastLogin; }
    public void setLastLogin(LocalDateTime t)  { this.lastLogin = t; }
    public LocalDateTime getCreatedAt()        { return createdAt; }
    public void setCreatedAt(LocalDateTime t)  { this.createdAt = t; }
    public String getActivityLog()             { return activityLog; }
    public void setActivityLog(String a)       { this.activityLog = a; }
}
