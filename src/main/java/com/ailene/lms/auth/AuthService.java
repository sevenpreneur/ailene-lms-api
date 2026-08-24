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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final AccessRepository accessRepository;

    @Transactional
    public AuthLoginResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleUserInfo userInfo = googleTokenVerifier.verify(request.accessToken());

        User user = userRepository.findByEmail(userInfo.email())
                .orElseThrow(() -> new ForbiddenException("This Google account is not registered as an LMS user"));

        user.setAvatar(userInfo.picture());
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
