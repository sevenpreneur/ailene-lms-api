package com.ailene.lms.auth;

import com.ailene.lms.common.exception.UnauthorizedException;
import com.ailene.lms.common.response.ApiResponse;
import com.ailene.lms.user.UserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${security.secret-key}")
    private String secretKey;

    @PostMapping("/login/google")
    public ResponseEntity<ApiResponse<AuthLoginResponse>> loginWithGoogle(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody GoogleLoginRequest request) {
        requireValidSecretKey(authorization);
        AuthLoginResponse response = authService.loginWithGoogle(request);
        return ApiResponse.success(HttpStatus.OK, "login successful", response);
    }

    @PostMapping("/check-session")
    public ResponseEntity<ApiResponse<UserDto>> checkSession(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        UserDto user = authService.checkSession(extractBearerToken(authorization));
        return ApiResponse.success(HttpStatus.OK, "session is valid", user);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        authService.logout(extractBearerToken(authorization));
        return ApiResponse.success(HttpStatus.OK, "logout successful", null);
    }

    private void requireValidSecretKey(String authorization) {
        if (!extractBearerToken(authorization).equals(secretKey)) {
            throw new UnauthorizedException("Bearer token is invalid");
        }
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or invalid authorization header");
        }
        return authorization.substring("Bearer ".length());
    }
}
