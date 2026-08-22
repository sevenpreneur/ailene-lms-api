package com.ailene.lms.auth;

import com.ailene.lms.common.exception.ForbiddenException;
import com.ailene.lms.user.User;
import com.ailene.lms.user.UserDto;
import com.ailene.lms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;

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
}
