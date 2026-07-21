package com.taskflow.userservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.userservice.entity.UserProfile;
import com.taskflow.userservice.repository.UserProfileRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_VALIDITY_MINUTES = 5;
    private static final String UPLOAD_DIR = "./profile-photos/";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserProfileRepository repository;
    private final JavaMailSender mailSender;
    // We store hashed passwords in auth-service; here we only verify/change via OTP flow
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /** Sender address. Must match the authenticated SMTP account for Gmail/most providers. */
    @org.springframework.beans.factory.annotation.Value("${spring.mail.username:noreply@taskflow.com}")
    private String fromAddress;

    public UserProfileService(UserProfileRepository repository, JavaMailSender mailSender) {
        this.repository = repository;
        this.mailSender = mailSender;
    }

    // ── Existing methods (unchanged) ──────────────────────────────────────

    public UserProfile saveOrUpdate(UserProfile profile) {
        Optional<UserProfile> existing = (profile.getEmail() == null)
                ? Optional.empty()
                : repository.findByEmail(profile.getEmail());
        if (existing.isPresent()) {
            UserProfile current = existing.get();
            current.setUsername(profile.getUsername());
            if (profile.getRole() != null)        current.setRole(profile.getRole());
            if (profile.getStatus() != null)      current.setStatus(profile.getStatus());
            if (profile.getDepartment() != null)  current.setDepartment(profile.getDepartment());
            if (profile.getManagerEmail() != null) current.setManagerEmail(profile.getManagerEmail());
            return repository.save(current);
        }
        return repository.save(profile);
    }

    public List<UserProfile> getAll() { return repository.findAll(); }

    /**
     * Users visible to the caller.
     *   SUPER_ADMIN / ADMIN -> everyone
     *   MANAGER             -> only their direct reports (managerEmail = their email)
     *   USER                -> only themselves
     */
    public List<UserProfile> getVisibleUsers(String callerEmail, String callerRole) {
        String role = callerRole == null ? "" : callerRole.toUpperCase();
        if ("SUPER_ADMIN".equals(role) || "ADMIN".equals(role)) {
            return repository.findAll();
        }
        if ("MANAGER".equals(role)) {
            return repository.findByManagerEmail(callerEmail);
        }
        List<UserProfile> self = new ArrayList<>();
        repository.findByEmail(callerEmail).ifPresent(self::add);
        return self;
    }

    /** Direct reports of a given manager — used by the admin Managers page. */
    public List<UserProfile> getTeamOf(String managerEmail) {
        if (managerEmail == null || managerEmail.isBlank()) return new ArrayList<>();
        return repository.findByManagerEmail(managerEmail);
    }

    /** Internal: update approval status synced from auth-service. */
    @Transactional
    public void updateStatus(String email, String status) {
        if (email == null || status == null) return;
        repository.findByEmail(email).ifPresent(p -> {
            p.setStatus(status);
            repository.save(p);
        });
    }

    /** Manager profiles, optionally filtered by status. */
    public List<UserProfile> getManagers(String status) {
        List<UserProfile> managers = repository.findByRole("MANAGER");
        if (status == null || status.isBlank()) {
            return managers;
        }
        String wanted = status.toUpperCase();
        List<UserProfile> filtered = new ArrayList<>();
        for (UserProfile m : managers) {
            if (wanted.equals(m.getStatus())) filtered.add(m);
        }
        return filtered;
    }

    public UserProfile findByEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }

    public void deleteById(Long id) {
        if (!repository.existsById(id)) throw new IllegalArgumentException("User not found: " + id);
        repository.deleteById(id);
    }

    // ── New profile methods ───────────────────────────────────────────────

    /** GET /api/users/profile */
    public UserProfile getProfile(String email) {
        return findByEmail(email);
    }

    /** PUT /api/users/profile */
    @Transactional
    public UserProfile updateProfile(String email, String newUsername, String bio) {
        UserProfile profile = findByEmail(email);
        if (newUsername != null && !newUsername.isBlank()) {
            profile.setUsername(newUsername.trim());
        }
        profile.setBio(bio);
        addActivity(profile, "Profile updated");
        return repository.save(profile);
    }

    /** POST /api/users/send-otp — sends OTP to user's email */
    @Transactional
    public void sendOtp(String email) {
        UserProfile profile = findByEmail(email);
        String otp = String.valueOf(RANDOM.nextInt(900_000) + 100_000);
        profile.setOtp(otp);
        profile.setOtpExpiry(LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES));
        repository.save(profile);
        sendOtpEmail(email, otp);
        log.info("OTP sent to {}", email);
    }

    /** POST /api/users/verify-otp */
    public boolean verifyOtp(String email, String otp) {
        UserProfile profile = findByEmail(email);
        if (profile.getOtp() == null || profile.getOtpExpiry() == null) return false;
        if (LocalDateTime.now().isAfter(profile.getOtpExpiry())) return false;
        return profile.getOtp().equals(otp);
    }

    /** POST /api/users/change-password — OTP already verified, set new password hash */
    @Transactional
    public void changePassword(String email, String otp, String newPassword) {
        if (!verifyOtp(email, otp)) throw new IllegalArgumentException("Invalid or expired OTP");
        UserProfile profile = findByEmail(email);
        // Clear OTP after use
        profile.setOtp(null);
        profile.setOtpExpiry(null);
        addActivity(profile, "Password changed");
        repository.save(profile);
        // NOTE: actual credential update must go to auth-service.
        // This endpoint records the activity and clears the OTP.
    }

    /** POST /api/users/upload-photo */
    @Transactional
    public String uploadPhoto(String email, MultipartFile file) throws IOException {
        UserProfile profile = findByEmail(email);
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

        String ext = "";
        String orig = file.getOriginalFilename();
        if (orig != null && orig.contains(".")) ext = orig.substring(orig.lastIndexOf('.'));
        String filename = UUID.randomUUID() + ext;
        Files.copy(file.getInputStream(), uploadPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);

        // Delete old photo
        if (profile.getProfileImage() != null) {
            try { Files.deleteIfExists(uploadPath.resolve(profile.getProfileImage())); } catch (Exception ignored) {}
        }

        profile.setProfileImage(filename);
        addActivity(profile, "Profile photo updated");
        repository.save(profile);
        return filename;
    }

    /** DELETE /api/users/delete-account — OTP verified */
    @Transactional
    public void deleteAccount(String email, String otp) {
        if (!verifyOtp(email, otp)) throw new IllegalArgumentException("Invalid or expired OTP");
        UserProfile profile = findByEmail(email);
        repository.delete(profile);
        log.info("Account deleted for {}", email);
    }

    /** Update last_login timestamp */
    @Transactional
    public void recordLogin(String email) {
        repository.findByEmail(email).ifPresent(p -> {
            p.setLastLogin(LocalDateTime.now());
            addActivity(p, "Logged in");
            repository.save(p);
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void addActivity(UserProfile profile, String action) {
        try {
            List<Map<String, String>> activities = new ArrayList<>();
            if (profile.getActivityLog() != null && !profile.getActivityLog().isBlank()) {
                activities = MAPPER.readValue(profile.getActivityLog(),
                        new TypeReference<List<Map<String, String>>>() {});
            }
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("action", action);
            entry.put("time", LocalDateTime.now().format(FMT));
            activities.add(0, entry); // newest first
            if (activities.size() > 20) activities = activities.subList(0, 20); // keep last 20
            profile.setActivityLog(MAPPER.writeValueAsString(activities));
        } catch (Exception e) {
            log.warn("Failed to update activity log: {}", e.getMessage());
        }
    }

    private void sendOtpEmail(String toEmail, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, "TaskFlow");
            helper.setTo(toEmail);
            helper.setSubject("TaskFlow — Your OTP Code");
            helper.setText(
                "<div style='font-family:Arial,sans-serif;padding:20px'>" +
                "<h2>Your OTP Code</h2>" +
                "<p>Use the code below to proceed:</p>" +
                "<div style='font-size:32px;font-weight:bold;letter-spacing:8px;color:#6378ff;padding:15px;background:#f0f2ff;border-radius:8px;text-align:center'>" + otp + "</div>" +
                "<p style='color:#888;margin-top:16px'>This OTP expires in 5 minutes.</p>" +
                "</div>", true);
            mailSender.send(message);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send OTP email: {}", e.getMessage());
            throw new IllegalStateException("Failed to send OTP email");
        }
    }
}
