package com.ailene.lms.auth;

import com.ailene.lms.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login/google")
    public ResponseEntity<ApiResponse<AuthLoginResponse>> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        AuthLoginResponse response = authService.loginWithGoogle(request);
        return ApiResponse.success(HttpStatus.OK, "login successful", response);
    }
}
