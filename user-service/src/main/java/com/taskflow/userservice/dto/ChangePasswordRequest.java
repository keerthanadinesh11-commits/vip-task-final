package com.taskflow.userservice.dto;

public class ChangePasswordRequest {
    private String oldPassword;
    private String otp;
    private String newPassword;
    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String o) { this.oldPassword = o; }
    public String getOtp() { return otp; }
    public void setOtp(String o) { this.otp = o; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String n) { this.newPassword = n; }
}
