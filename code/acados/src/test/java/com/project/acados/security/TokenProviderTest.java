package com.project.acados.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TokenProviderTest {

    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    private TokenProvider tokenProvider;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        tokenProvider = new TokenProvider(SECRET, EXPIRATION_MS);
        userDetails = new User(
                "T001",
                "passwordHash",
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
        );
    }

    @Test
    @DisplayName("generateToken and extractUsername: should create token and extract matching username")
    void testGenerateTokenAndExtractUsername() {
        String token = tokenProvider.generateToken(userDetails);

        assertThat(token).isNotBlank();
        String extractedUsername = tokenProvider.extractUsername(token);
        assertThat(extractedUsername).isEqualTo("T001");
    }

    @Test
    @DisplayName("isTokenValid: should return true for valid token matching user details")
    void testIsTokenValid_Success() {
        String token = tokenProvider.generateToken(userDetails);

        boolean valid = tokenProvider.isTokenValid(token, userDetails);

        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("isTokenValid: should return false when username in token does not match user details")
    void testIsTokenValid_DifferentUser() {
        String token = tokenProvider.generateToken(userDetails);
        UserDetails otherUser = new User(
                "S001",
                "passwordHash",
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );

        boolean valid = tokenProvider.isTokenValid(token, otherUser);

        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("isTokenValid: should return false for expired token")
    void testIsTokenValid_Expired() {
        TokenProvider shortLivedProvider = new TokenProvider(SECRET, -1000); // already expired
        String token = shortLivedProvider.generateToken(userDetails);

        boolean valid = shortLivedProvider.isTokenValid(token, userDetails);

        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("isTokenValid: should return false for malformed or tampered token")
    void testIsTokenValid_Malformed() {
        boolean valid = tokenProvider.isTokenValid("invalid.jwt.token", userDetails);

        assertThat(valid).isFalse();
    }
}

