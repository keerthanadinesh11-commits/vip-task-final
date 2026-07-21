package com.taskflow.userservice.dto;

public class UpdateProfileRequest {
    private String username;
    private String bio;
    public String getUsername() { return username; }
    public void setUsername(String u) { this.username = u; }
    public String getBio() { return bio; }
    public void setBio(String b) { this.bio = b; }
}
