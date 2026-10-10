package com.project.acados.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtFilter;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        userDetails = new User(
                "T001",
                "passwordHash",
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("doFilterInternal: should authenticate when Authorization header has valid Bearer token")
    void testDoFilterInternal_WithValidAuthorizationHeader() throws ServletException, IOException {
        String token = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenProvider.extractUsername(token)).thenReturn("T001");
        when(userDetailsService.loadUserByUsername("T001")).thenReturn(userDetails);
        when(tokenProvider.isTokenValid(token, userDetails)).thenReturn(true);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("T001");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal: should authenticate when cookie has valid token")
    void testDoFilterInternal_WithValidCookieToken() throws ServletException, IOException {
        String token = "valid.cookie.token";
        when(request.getHeader("Authorization")).thenReturn(null);
        Cookie tokenCookie = new Cookie(JwtAuthenticationFilter.TOKEN_COOKIE_NAME, token);
        when(request.getCookies()).thenReturn(new Cookie[]{tokenCookie});
        when(tokenProvider.extractUsername(token)).thenReturn("T001");
        when(userDetailsService.loadUserByUsername("T001")).thenReturn(userDetails);
        when(tokenProvider.isTokenValid(token, userDetails)).thenReturn(true);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("T001");

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal: should not authenticate when token is invalid")
    void testDoFilterInternal_WithInvalidToken() throws ServletException, IOException {
        String token = "invalid.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenProvider.extractUsername(token)).thenReturn("T001");
        when(userDetailsService.loadUserByUsername("T001")).thenReturn(userDetails);
        when(tokenProvider.isTokenValid(token, userDetails)).thenReturn(false);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal: should proceed down filter chain when no token is present")
    void testDoFilterInternal_NoToken() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(tokenProvider, userDetailsService);
    }
}

