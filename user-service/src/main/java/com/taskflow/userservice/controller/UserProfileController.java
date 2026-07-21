package com.taskflow.userservice.controller;

import com.taskflow.userservice.dto.ChangePasswordRequest;
import com.taskflow.userservice.dto.UpdateProfileRequest;
import com.taskflow.userservice.dto.UserProfileDto;
import com.taskflow.userservice.dto.VerifyOtpRequest;
import com.taskflow.userservice.entity.UserProfile;
import com.taskflow.userservice.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@Tag(name = "User Profiles", description = "Profile management, photo upload, OTP, account deletion")
@SecurityRequirement(name = "Bearer Authentication")
public class UserProfileController {

    private final UserProfileService service;

    public UserProfileController(UserProfileService service) {
        this.service = service;
    }

    // ── Existing endpoints (unchanged) ────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','USER')")
    @Operation(summary = "List user profiles visible to the caller",
            description = "SUPER_ADMIN sees everyone; MANAGER sees only their direct reports; "
                    + "USER sees only themselves.")
    public ResponseEntity<List<UserProfileDto>> getAll(Authentication auth) {
        return ResponseEntity.ok(
                service.getVisibleUsers(auth.getName(), currentRole(auth))
                        .stream().map(this::toDto).toList());
    }

    @GetMapping("/team")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER')")
    @Operation(summary = "List a manager's direct reports",
            description = "SUPER_ADMIN may pass ?managerEmail= to inspect any manager's team. "
                    + "A MANAGER always gets their own team regardless of the parameter.")
    public ResponseEntity<List<UserProfileDto>> getTeam(
            @org.springframework.web.bind.annotation.RequestParam(
                    value = "managerEmail", required = false) String managerEmail,
            Authentication auth) {
        String role = currentRole(auth);
        String target = ("SUPER_ADMIN".equals(role) || "ADMIN".equals(role))
                ? (managerEmail == null || managerEmail.isBlank() ? auth.getName() : managerEmail)
                : auth.getName();
        return ResponseEntity.ok(service.getTeamOf(target).stream().map(this::toDto).toList());
    }

    @GetMapping("/all-users")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "List all user profiles (ADMIN view)")
    public List<UserProfileDto> getAllUsers() {
        return service.getAll().stream().map(this::toDto).toList();
    }

    @PostMapping("/internal/create")
    @Operation(summary = "Internal: receive a synced profile from auth-service")
    public ResponseEntity<UserProfileDto> createInternal(@RequestBody Map<String, String> body) {
        UserProfile profile = new UserProfile();
        profile.setUsername(body.get("username"));
        profile.setEmail(body.get("email"));
        profile.setRole(body.getOrDefault("role", "USER"));
        profile.setStatus(body.getOrDefault("status", "APPROVED"));
        profile.setDepartment(body.get("department"));
        profile.setManagerEmail(body.get("managerEmail"));
        UserProfile saved = service.saveOrUpdate(profile);
        return ResponseEntity.ok(toDto(saved));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Delete a user profile by ID (ADMIN only)")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    // ── New profile endpoints ─────────────────────────────────────────────

    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','USER')")
    @Operation(summary = "Get my profile")
    public ResponseEntity<UserProfileDto> getProfile(Authentication auth) {
        return ResponseEntity.ok(toDto(service.getProfile(auth.getName())));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','USER')")
    @Operation(summary = "Update my profile (username, bio)")
    public ResponseEntity<UserProfileDto> updateProfile(
            @RequestBody UpdateProfileRequest req, Authentication auth) {
        UserProfile updated = service.updateProfile(auth.getName(), req.getUsername(), req.getBio());
        return ResponseEntity.ok(toDto(updated));
    }

    @PostMapping("/send-otp")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','USER')")
    @Operation(summary = "Send OTP to my email for password change / account deletion")
    public ResponseEntity<Map<String, String>> sendOtp(Authentication auth) {
        service.sendOtp(auth.getName());
        return ResponseEntity.ok(Map.of("message", "OTP sent to your registered email"));
    }

    @PostMapping("/verify-otp")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','USER')")
    @Operation(summary = "Verify OTP")
    public ResponseEntity<Map<String, String>> verifyOtp(
            @RequestBody VerifyOtpRequest req, Authentication auth) {
        boolean ok = service.verifyOtp(auth.getName(), req.getOtp());
        if (!ok) return ResponseEntity.badRequest().body(Map.of("message", "Invalid or expired OTP"));
        return ResponseEntity.ok(Map.of("message", "OTP verified successfully"));
    }

    @PostMapping("/change-password")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','USER')")
    @Operation(summary = "Change password after OTP verification")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestBody ChangePasswordRequest req, Authentication auth) {
        service.changePassword(auth.getName(), req.getOtp(), req.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @PostMapping(value = "/upload-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','USER')")
    @Operation(summary = "Upload profile photo")
    public ResponseEntity<Map<String, String>> uploadPhoto(
            @RequestPart("file") MultipartFile file, Authentication auth) throws IOException {
        String filename = service.uploadPhoto(auth.getName(), file);
        return ResponseEntity.ok(Map.of("filename", filename, "message", "Photo uploaded successfully"));
    }

    @GetMapping("/profile-photo/{filename}")
    @Operation(summary = "Serve profile photo")
    public ResponseEntity<org.springframework.core.io.Resource> getPhoto(@PathVariable String filename) {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get("./profile-photos/").resolve(filename);
            org.springframework.core.io.Resource resource =
                    new org.springframework.core.io.UrlResource(path.toUri());
            if (!resource.exists()) return ResponseEntity.notFound().build();
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/delete-account")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','USER')")
    @Operation(summary = "Delete my account after OTP verification")
    public ResponseEntity<Map<String, String>> deleteAccount(
            @RequestBody VerifyOtpRequest req, Authentication auth) {
        service.deleteAccount(auth.getName(), req.getOtp());
        return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
    }

    @PostMapping("/internal/record-login")
    @Operation(summary = "Internal: record last login time")
    public ResponseEntity<Void> recordLogin(@RequestBody Map<String, String> body) {
        service.recordLogin(body.get("email"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/internal/update-status")
    @Operation(summary = "Internal: update a profile's approval status")
    public ResponseEntity<Void> updateStatus(@RequestBody Map<String, String> body) {
        service.updateStatus(body.get("email"), body.get("status"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/managers")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "List manager profiles, optionally by status")
    public List<UserProfileDto> getManagers(
            @org.springframework.web.bind.annotation.RequestParam(value = "status", required = false) String status) {
        return service.getManagers(status).stream().map(this::toDto).toList();
    }

    // ── Helper ────────────────────────────────────────────────────────────

    /** Extracts the caller's role name (without the ROLE_ prefix) from the JWT authorities. */
    private String currentRole(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) return "";
        for (org.springframework.security.core.GrantedAuthority a : auth.getAuthorities()) {
            String v = a.getAuthority();
            if (v == null) continue;
            if (v.startsWith("ROLE_")) v = v.substring(5);
            return v.toUpperCase();
        }
        return "";
    }

    private UserProfileDto toDto(UserProfile p) {
        UserProfileDto dto = new UserProfileDto(p.getId(), p.getUsername(), p.getEmail(), p.getRole());
        dto.setStatus(p.getStatus());
        dto.setDepartment(p.getDepartment());
        dto.setManagerEmail(p.getManagerEmail());
        dto.setBio(p.getBio());
        dto.setProfileImage(p.getProfileImage());
        dto.setLastLogin(p.getLastLogin());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setActivityLog(p.getActivityLog());
        return dto;
    }
}
