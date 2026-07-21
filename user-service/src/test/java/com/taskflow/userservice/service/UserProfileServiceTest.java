package com.taskflow.userservice.service;

import com.taskflow.userservice.entity.UserProfile;
import com.taskflow.userservice.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock private UserProfileRepository repository;
    @InjectMocks private UserProfileService service;

    @Test
    void saveOrUpdateInsertsWhenEmailNotPresent() {
        UserProfile p = new UserProfile(null, "alice", "a@x.com", "USER");
        when(repository.findByEmail("a@x.com")).thenReturn(Optional.empty());
        when(repository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfile saved = service.saveOrUpdate(p);

        assertEquals("alice", saved.getUsername());
        verify(repository).save(p);
    }

    @Test
    void saveOrUpdateUpdatesExistingByEmail() {
        UserProfile existing = new UserProfile(1L, "old", "a@x.com", "USER");
        UserProfile updated = new UserProfile(null, "new", "a@x.com", "MANAGER");
        when(repository.findByEmail("a@x.com")).thenReturn(Optional.of(existing));
        when(repository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfile saved = service.saveOrUpdate(updated);

        assertEquals("new", saved.getUsername());
        assertEquals("MANAGER", saved.getRole());
        assertEquals(1L, saved.getId());
    }

    @Test
    void getAllReturnsRepositoryList() {
        when(repository.findAll()).thenReturn(List.of(new UserProfile(1L, "a", "a@x.com", "USER")));
        assertEquals(1, service.getAll().size());
    }

    @Test
    void findByEmailThrowsWhenMissing() {
        when(repository.findByEmail("missing@x.com")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.findByEmail("missing@x.com"));
    }

    @Test
    void findByEmailReturnsProfile() {
        UserProfile p = new UserProfile(1L, "a", "a@x.com", "USER");
        when(repository.findByEmail("a@x.com")).thenReturn(Optional.of(p));
        assertEquals("a", service.findByEmail("a@x.com").getUsername());
    }

    @Test
    void deleteByIdDeletesWhenExists() {
        when(repository.existsById(1L)).thenReturn(true);
        service.deleteById(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    void deleteByIdThrowsWhenNotFound() {
        when(repository.existsById(99L)).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> service.deleteById(99L));
    }
}
