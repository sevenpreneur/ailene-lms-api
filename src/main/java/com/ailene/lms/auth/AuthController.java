package com.ailene.lms.auth;

import com.ailene.lms.common.response.ApiResponse;
import com.ailene.lms.common.security.SecretKeyGuard;
import com.ailene.lms.user.UserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping("/login/google")
    public ResponseEntity<ApiResponse<AuthLoginResponse>> loginWithGoogle(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody GoogleLoginRequest request) {
        secretKeyGuard.requireValidSecretKey(authorization);
        AuthLoginResponse response = authService.loginWithGoogle(request);
        return ApiResponse.success(HttpStatus.OK, "login successful", response);
    }

    @PostMapping("/check-session")
    public ResponseEntity<ApiResponse<UserDto>> checkSession(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        UserDto user = authService.checkSession(secretKeyGuard.extractBearerToken(authorization));
        return ApiResponse.success(HttpStatus.OK, "session is valid", user);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        authService.logout(secretKeyGuard.extractBearerToken(authorization));
        return ApiResponse.success(HttpStatus.OK, "logout successful", null);
    }
}
