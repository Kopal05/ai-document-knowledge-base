package com.kopal.smartknowledgebase.security;

import com.kopal.smartknowledgebase.entity.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String VALID_SECRET =
            "a-random-at-least-32-byte-long-development-secret-value-change-me";

    private CustomUserDetails userDetails(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setName("Test User");
        user.setEmail(email);
        user.setPasswordHash("hashed");
        return new CustomUserDetails(user);
    }

    @Test
    void generatesAndValidatesTokenRoundTrip() {
        JwtService jwtService = new JwtService(VALID_SECRET, 3600000);
        CustomUserDetails user = userDetails(1L, "test@example.com");

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractEmail(token)).isEqualTo("test@example.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo(1L);
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void tokenIsInvalidForDifferentUser() {
        JwtService jwtService = new JwtService(VALID_SECRET, 3600000);
        CustomUserDetails user = userDetails(1L, "user1@example.com");
        CustomUserDetails otherUser = userDetails(2L, "user2@example.com");

        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    void expiredTokenIsInvalid() throws InterruptedException {
        JwtService jwtService = new JwtService(VALID_SECRET, 1);
        CustomUserDetails user = userDetails(1L, "test@example.com");

        String token = jwtService.generateToken(user);
        Thread.sleep(10);

        assertThat(jwtService.isTokenValid(token, user)).isFalse();
    }

    @Test
    void malformedTokenIsRejectedByIsTokenValid() {
        JwtService jwtService = new JwtService(VALID_SECRET, 3600000);
        CustomUserDetails user = userDetails(1L, "test@example.com");

        assertThat(jwtService.isTokenValid("not-a-real-token", user)).isFalse();
    }

    @Test
    void tokenSignedWithDifferentKeyIsRejected() {
        JwtService jwtServiceA = new JwtService(VALID_SECRET, 3600000);
        JwtService jwtServiceB = new JwtService(
                "a-different-random-at-least-32-byte-long-secret-value", 3600000);
        CustomUserDetails user = userDetails(1L, "test@example.com");

        String token = jwtServiceA.generateToken(user);

        assertThat(jwtServiceB.isTokenValid(token, user)).isFalse();
    }

    @Test
    void constructorThrowsForBlankSecret() {
        assertThatThrownBy(() -> new JwtService("", 3600000))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void constructorThrowsForTooShortSecret() {
        assertThatThrownBy(() -> new JwtService("too-short", 3600000))
                .isInstanceOf(IllegalStateException.class);
    }
}