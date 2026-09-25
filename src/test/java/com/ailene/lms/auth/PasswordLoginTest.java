package com.ailene.lms.auth;

import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.common.exception.UnauthorizedException;
import com.ailene.lms.user.User;
import com.ailene.lms.user.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordLoginTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final TokenRepository tokenRepository = mock(TokenRepository.class);
    private final PasswordHasher hasher = new PasswordHasher();
    private final AuthService authService = new AuthService(mock(GoogleTokenVerifier.class), jwtService,
            userRepository, tokenRepository, mock(AccessRepository.class), hasher);

    @Test
    void loginWithPassword_correctPassword_issuesTrackedSession() {
        User user = user(hasher.hash("rahasia-123"));
        when(jwtService.issue(user)).thenReturn("jwt-abc");

        AuthLoginResponse response = authService.loginWithPassword(
                new PasswordLoginRequest(" Rani@Example.com ", "rahasia-123"));

        assertThat(response.token()).isEqualTo("jwt-abc");
        assertThat(user.getLastActiveAt()).isNotNull();
        verify(tokenRepository).save(any(Token.class));
    }

    @Test
    void loginWithPassword_wrongPasswordUnknownEmailOrNoPassword_allLookTheSame() {
        user(hasher.hash("rahasia-123"));
        assertRejected(new PasswordLoginRequest("rani@example.com", "salah"));

        when(userRepository.findByEmailIgnoreCase("siapa@example.com")).thenReturn(Optional.empty());
        assertRejected(new PasswordLoginRequest("siapa@example.com", "rahasia-123"));

        user(null);
        assertRejected(new PasswordLoginRequest("rani@example.com", "rahasia-123"));
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void hasher_neverStoresThePlainPassword() {
        String hash = hasher.hash("rahasia-123");

        assertThat(hash).startsWith("$2").doesNotContain("rahasia");
        assertThat(hasher.matches("rahasia-123", hash)).isTrue();
        assertThat(hasher.matches("x".repeat(40) + "é".repeat(32), hash)).isFalse();
    }

    private void assertRejected(PasswordLoginRequest request) {
        assertThatThrownBy(() -> authService.loginWithPassword(request))
                .isInstanceOf(UnauthorizedException.class).hasMessage("Invalid email or password");
    }

    private User user(String passwordHash) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("rani@example.com");
        user.setFullName("Rani");
        user.setJobTitle("HR");
        user.setPasswordHash(passwordHash);
        when(userRepository.findByEmailIgnoreCase("rani@example.com")).thenReturn(Optional.of(user));
        return user;
    }
}
