
package com.springboot.stockpilot.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private UserDetailsService userDetailsService;
    private JwtAuthenticationFilter filter;
    private Claims claims;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        jwtService = mock(JwtService.class);
        userDetailsService = mock(UserDetailsService.class);
        claims = mock(Claims.class);
        filterChain = mock(FilterChain.class);

        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateWithValidToken() throws Exception {
        MockHttpServletRequest request = requestWithToken("valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        UserDetails user = User.withUsername("customer@example.com")
                .password("encoded-password")
                .authorities("ROLE_CUSTOMER")
                .build();

        when(jwtService.parseToken("valid-token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("customer@example.com");
        when(userDetailsService.loadUserByUsername("customer@example.com"))
                .thenReturn(user);

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(
                "customer@example.com",
                SecurityContextHolder.getContext().getAuthentication().getName()
        );
        assertEquals(200, response.getStatus());

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldRejectInvalidOrExpiredToken() throws Exception {
        MockHttpServletRequest request = requestWithToken("invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.parseToken("invalid-token"))
                .thenThrow(new JwtException("Invalid or expired token"));

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(filterChain, never()).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void shouldRejectInactiveUser() throws Exception {
        MockHttpServletRequest request = requestWithToken("valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        UserDetails inactiveUser = User.withUsername("customer@example.com")
                .password("encoded-password")
                .authorities("ROLE_CUSTOMER")
                .disabled(true)
                .build();

        when(jwtService.parseToken("valid-token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("customer@example.com");
        when(userDetailsService.loadUserByUsername("customer@example.com"))
                .thenReturn(inactiveUser);

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void shouldRejectTokenForUnknownUser() throws Exception {
        MockHttpServletRequest request = requestWithToken("valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.parseToken("valid-token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("missing@example.com");
        when(userDetailsService.loadUserByUsername("missing@example.com"))
                .thenThrow(new UsernameNotFoundException("User not found"));

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void shouldContinueWhenAuthorizationHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
