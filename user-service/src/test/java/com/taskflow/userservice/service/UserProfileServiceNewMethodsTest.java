package com.taskflow.userservice.service;

import com.taskflow.userservice.entity.UserProfile;
import com.taskflow.userservice.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.internet.MimeMessage;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceNewMethodsTest {

    @Mock private UserProfileRepository repository;
    @Mock private JavaMailSender mailSender;
    @Mock private MimeMessage mimeMessage;
    @InjectMocks private UserProfileService service;

    private UserProfile profile;

    @BeforeEach
    void setUp() {
        profile = new UserProfile(1L, "alice", "alice@x.com", "USER");
    }

    // ── getProfile ────────────────────────────────────────────────────────────

    @Test
    void getProfile_returnsProfile_whenFound() {
        when(repository.findByEmail("alice@x.com")).thenReturn(Optional.of(profile));
        UserProfile result = service.getProfile("alice@x.com");
        assertEquals("alice", result.getUsername());
    }

    @Test
    void getProfile_throws_whenNotFound() {
        when(repository.findByEmail("missing@x.com")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.getProfile("missing@x.com"));
    }

    // ── updateProfile ─────────────────────────────────────────────────────────

    @Test
    void updateProfile_updatesUsernameAndBio() {
        when(repository.findByEmail("alice@x.com")).thenReturn(Optional.of(profile));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserProfile result = service.updateProfile("alice@x.com", "alice2", "My bio");

        assertEquals("alice2", result.getUsername());
        assertEquals("My bio", result.getBio());
        verify(repository).save(profile);
    }

    @Test
    void updateProfile_keepsOldUsername_whenNewIsBlank() {
        when(repository.findByEmail("alice@x.com")).thenReturn(Optional.of(profile));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserProfile result = service.updateProfile("alice@x.com", "  ", "bio only");

        assertEquals("alice", result.getUsername());
        assertEquals("bio only", result.getBio());
    }

    @Test
    void updateProfile_keepsOldUsername_whenNewIsNull() {
        when(repository.findByEmail("alice@x.com")).thenReturn(Optional.of(profile));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserProfile result = service.updateProfile("alice@x.com", null, "some bio");

        assertEquals("alice", result.getUsername());
    }

    // ── sendOtp ───────────────────────────────────────────────────────────────

    @Test
    void sendOtp_setsOtpAndExpiry() {
        when(repository.findByEmail("alice@x.com")).thenReturn(Optional.of(profile));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        service.sendOtp("alice@x.com");

        assertNotNull(profile.getOtp());
        assertEquals(6, profile.getOtp().length());
        assertNotNull(profile.getOtpExpiry());
        assertTrue(profile.getOtpExpiry().isAfter(LocalDateTime.now()));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendOtp_throwsWhenUserNotFound() {
        when(repository.findByEmail("missing@x.com")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.sendOtp("missing@x.com"));
    }

    // ── verifyOtp ─────────────────────────────────────────────────────────────

    @Test
    void verifyOtp_returnsTrue_whenOtpMatchesAndNotExpired() {
        profile.setOtp("123456");
        profile.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        when(repository.findByEmail("alice@x.com")).thenReturn(Optional.of(profile));

        assertTrue(service.verifyOtp("alice@x.com", "123456"));
    }

    @Test
    void verifyOtp_returnsFalse_whenOtpDoesNotMatch() {
        profile.setOtp("123456");
        profile.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        when(repository.findByEmail("alice@x.com")).thenReturn(Optional.of(profile));

        assertFalse(service.verifyOtp("alice@x.com", "999999"));
    }

    @Test
    void verifyOtp_returnsFalse_whenExpired() {
        profile.setOtp("123456");
        profile.setOtpExpiry(LocalDateTime.now().minusMinutes(1));
        when(repository.findByEmail("alice@x.com")).thenReturn(Optional.of(profile));

        assertFalse(service.verifyOtp("alice@x.com", "123456"));
    }

    @Test
    void verifyOtp_returnsFalse_whenOtpIsNull() {
        profile.setOtp(null);
        profile.setOtpExpiry(null);
        when(repository.findByEmail("alice@x.com")).thenReturn(Optional.of(profile));

        assertFalse(service.verifyOtp("alice@x.com", "123456"));
    }

    @Test
    void verifyOtp_returnsFalse_whenExpiryIsNull() {
        profile.setOtp("123456");
        profile.setOtpExpiry(null);
        when(repository.findByEmail("alice@x.com")).thenReturn(Optional.of(profile));

        assertFalse(service.verifyOtp("alice@x.com", "123456"));
    }

    // ── changePassword ────────────────────────────────────────────────────────

    @Test
    void changePassword_clearsOtp_whenOtpValid() {
        profile.setOtp("123456");
        profile.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        when(repository.findByEmail("alice@x.com")).thenReturn(Optional.of(profile));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.changePassword("alice@x.com", "123456", "newPass123");

        assertNull(profile.getOtp());
        assertNull(profile.getOtpExpiry());
        verify(repository).save(profile);
    }

    @Test
    void changePassword_throws_whenOtpInvalid() {
        profile.setOtp("123456");
        profile.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        when(repository.findByEmail("alice@x.com")).thenReturn(Optional.of(profile));

        assertThrows(IllegalArgumentException.class,
                () -> service.changePassword("alice@x.com", "wrong", "newPass123"));
    }

    @Test
    void changePassword_throws_whenOtpExpired() {
        profile.setOtp("123456");
        profile.setOtpExpiry(LocalDateTime.now().minusMinutes(10));
        when(repository.findByEmail("alice@x.com")).thenReturn(Optional.of(profile));

        assertThrows(IllegalArgumentException.class,
                () -> service.changePassword("alice@x.com", "123456", "newPass123"));
    }

    // ── deleteAccount ─────────────────────────────────────────────────────────

    @Test
    void deleteAccount_deletesUser_whenOtpValid() {
        profile.setOtp("654321");
        profile.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        when(repository.findByEmail("alice@x.com")).thenReturn(Optional.of(profile));

        service.deleteAccount("alice@x.com", "654321");

        verify(repository).delete(profile);
    }

    @Test
    void deleteAccount_throws_whenOtpInvalid() {
        profile.setOtp("654321");
        profile.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        when(repository.findByEmail("alice@x.com")).thenReturn(Optional.of(profile));

        assertThrows(IllegalArgumentException.class,
                () -> service.deleteAccount("alice@x.com", "000000"));
        verify(repository, never()).delete(any());
    }

    // ── recordLogin ───────────────────────────────────────────────────────────

    @Test
    void recordLogin_updatesLastLogin_whenUserExists() {
        when(repository.findByEmail("alice@x.com")).thenReturn(Optional.of(profile));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.recordLogin("alice@x.com");

        assertNotNull(profile.getLastLogin());
        assertTrue(profile.getLastLogin().isBefore(LocalDateTime.now().plusSeconds(1)));
        verify(repository).save(profile);
    }

    @Test
    void recordLogin_doesNothing_whenUserNotFound() {
        when(repository.findByEmail("ghost@x.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.recordLogin("ghost@x.com"));
        verify(repository, never()).save(any());
    }

    // ── saveOrUpdate null role ────────────────────────────────────────────────

    @Test
    void saveOrUpdate_doesNotChangeRole_whenIncomingRoleIsNull() {
        UserProfile existing = new UserProfile(1L, "alice", "alice@x.com", "ADMIN");
        UserProfile incoming = new UserProfile(null, "alice-new", "alice@x.com", null);

        when(repository.findByEmail("alice@x.com")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserProfile result = service.saveOrUpdate(incoming);

        assertEquals("ADMIN", result.getRole());
        assertEquals("alice-new", result.getUsername());
    }
}
