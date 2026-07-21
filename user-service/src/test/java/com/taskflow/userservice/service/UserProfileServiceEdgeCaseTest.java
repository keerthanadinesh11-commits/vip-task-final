package com.taskflow.userservice.service;

import com.taskflow.userservice.entity.UserProfile;
import com.taskflow.userservice.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceEdgeCaseTest {

    @Mock UserProfileRepository repository;
    @InjectMocks UserProfileService service;

    @Test
    void saveOrUpdate_nullEmail_insertsWithoutLookup() {
        UserProfile p = new UserProfile(null, "anon", null, "USER");
        when(repository.save(any(UserProfile.class))).thenAnswer(inv -> {
            UserProfile arg = inv.getArgument(0);
            arg.setId(99L);
            return arg;
        });

        UserProfile saved = service.saveOrUpdate(p);
        assertEquals(99L, saved.getId());
        verify(repository).save(p);
    }

    @Test
    void saveOrUpdate_existingProfile_doesNotChangeEmailOnUpdate() {
        UserProfile existing = new UserProfile(1L, "old-name", "u@x.com", "USER");
        UserProfile incoming = new UserProfile(null, "new-name", "u@x.com", null);

        when(repository.findByEmail("u@x.com")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserProfile saved = service.saveOrUpdate(incoming);

        // role should remain unchanged when null passed in
        assertEquals("USER", saved.getRole());
        assertEquals("new-name", saved.getUsername());
        assertEquals(1L, saved.getId());
    }

    @Test
    void saveOrUpdate_existingProfile_updatesRoleWhenProvided() {
        UserProfile existing = new UserProfile(2L, "charlie", "c@x.com", "USER");
        UserProfile incoming = new UserProfile(null, "charlie", "c@x.com", "MANAGER");

        when(repository.findByEmail("c@x.com")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserProfile saved = service.saveOrUpdate(incoming);
        assertEquals("MANAGER", saved.getRole());
    }
}
