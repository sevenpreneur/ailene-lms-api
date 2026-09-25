package com.ailene.lms.auth;

import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.access.ProjectAccessDto;
import com.ailene.lms.common.exception.ForbiddenException;
import com.ailene.lms.common.exception.UnauthorizedException;
import com.ailene.lms.user.User;
import com.ailene.lms.user.UserDto;
import com.ailene.lms.user.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final AccessRepository accessRepository;
    private final PasswordHasher passwordHasher;

    @Transactional
    public AuthLoginResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleUserInfo userInfo = googleTokenVerifier.verify(request.accessToken());

        User user = userRepository.findByEmail(userInfo.email())
                .orElseThrow(() -> new ForbiddenException("This Google account is not registered as an LMS user"));

        user.setAvatar(userInfo.picture());
        return startSession(user);
    }

    // One message for unknown email, wrong password and no password set, so the endpoint can't confirm who exists.
    @Transactional
    public AuthLoginResponse loginWithPassword(PasswordLoginRequest request) {
        User user = userRepository
                .findByEmailIgnoreCase(request.email().trim().toLowerCase(Locale.ROOT)).orElse(null);
        String hash = user == null ? null : user.getPasswordHash();
        if (!passwordHasher.matches(request.password(), hash) || user == null) {
            throw new UnauthorizedException("Invalid email or password");
        }
        return startSession(user);
    }

    private AuthLoginResponse startSession(User user) {
        user.setLastActiveAt(OffsetDateTime.now());
        userRepository.save(user);

        String jwt = jwtService.issue(user);

        Token token = new Token();
        token.setUserId(user.getId());
        token.setToken(jwt);
        token.setActive(true);
        tokenRepository.save(token);

        return new AuthLoginResponse(jwt, UserDto.from(user));
    }

    public UUID resolveUserId(String jwt) {
        Claims claims = jwtService.parse(jwt);

        tokenRepository.findByTokenAndActiveTrue(jwt)
                .orElseThrow(() -> new UnauthorizedException("Session not found or already ended"));

        return UUID.fromString(claims.getSubject());
    }

    public CheckSessionResponse checkSession(String jwt) {
        UUID userId = resolveUserId(jwt);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Session not found or already ended"));

        List<ProjectAccessDto> projectAccess = accessRepository.findProjectAccessByUserId(userId).stream()
                .map(ProjectAccessDto::from)
                .toList();

        return new CheckSessionResponse(UserDto.from(user), projectAccess);
    }

    @Transactional
    public void logout(String jwt) {
        jwtService.parse(jwt);

        Token token = tokenRepository.findByToken(jwt)
                .orElseThrow(() -> new UnauthorizedException("Session not found or already ended"));

        tokenRepository.delete(token);
    }
}
