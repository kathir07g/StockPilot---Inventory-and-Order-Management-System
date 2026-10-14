package com.springboot.stockpilot.security;

import com.springboot.stockpilot.user.Role;
import com.springboot.stockpilot.user.User;
import com.springboot.stockpilot.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomUserDetailsServiceTest {

    private UserRepository userRepository;
    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userDetailsService = new CustomUserDetailsService(userRepository);
    }

    @Test
    void shouldLoadActiveUserWithRole() {
        User user = createUser(true, "ADMIN");

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("customer@example.com");

        assertEquals("customer@example.com", details.getUsername());
        assertTrue(details.isEnabled());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void shouldDisableInactiveUser() {
        User user = createUser(false, "CUSTOMER");

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("customer@example.com");

        assertFalse(details.isEnabled());
    }

    @Test
    void shouldNormalizeEmailBeforeDatabaseLookup() {
        User user = createUser(true, "CUSTOMER");

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("  CUSTOMER@EXAMPLE.COM  ");

        assertEquals("customer@example.com", details.getUsername());

        verify(userRepository).findByEmail("customer@example.com");
    }

    @Test
    void shouldRejectUnknownEmail() {
        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("missing@example.com"));
    }

    private User createUser(boolean active, String roleName) {
        Role role = new Role();
        role.setName(roleName);

        User user = new User();
        user.setName("Test User");
        user.setEmail("customer@example.com");
        user.setPasswordHash("encoded-password");
        user.setRole(role);
        user.setActive(active);

        return user;
    }
}