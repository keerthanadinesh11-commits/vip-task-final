package com.taskflow.auth.config;

import com.taskflow.auth.entity.UserCredential;
import com.taskflow.auth.repository.UserCredentialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserCredentialRepository repository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void loadUserByUsername_found_returnsUserDetails() {
        UserCredential cred = new UserCredential(1L, "alice", "alice@x.com", "hashed", "USER");
        when(repository.findByEmail("alice@x.com")).thenReturn(Optional.of(cred));

        UserDetails details = service.loadUserByUsername("alice@x.com");

        assertEquals("alice@x.com", details.getUsername());
        assertEquals("hashed", details.getPassword());
    }

    @Test
    void loadUserByUsername_notFound_throwsUsernameNotFoundException() {
        when(repository.findByEmail("missing@x.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("missing@x.com"));
    }
}
