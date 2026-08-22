package com.ailene.lms.service;

import com.ailene.lms.dto.AuthLoginResponse;
import com.ailene.lms.dto.GoogleLoginRequest;
import com.ailene.lms.dto.UserDto;
import com.ailene.lms.entity.Token;
import com.ailene.lms.entity.User;
import com.ailene.lms.exception.ForbiddenException;
import com.ailene.lms.repository.TokenRepository;
import com.ailene.lms.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public AuthLoginResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(request.idToken());
        String email = payload.getEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ForbiddenException("This Google account is not registered as an LMS user"));

        user.setAvatar((String) payload.get("picture"));
        user.setLastActiveAt(OffsetDateTime.now());
        userRepository.save(user);

        Token token = new Token();
        token.setUserId(user.getId());
        token.setToken(generateToken());
        token.setActive(true);
        tokenRepository.save(token);

        return new AuthLoginResponse(token.getToken(), UserDto.from(user));
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
