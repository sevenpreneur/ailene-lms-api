package com.ailene.lms.hello;

import com.ailene.lms.common.response.ApiResponse;
import com.ailene.lms.common.security.SecretKeyGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HelloController {

    private final SecretKeyGuard secretKeyGuard;

    @PostMapping("/hello-world")
    public ResponseEntity<ApiResponse<String>> helloWorld(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        secretKeyGuard.requireValidSecretKey(authorization);
        return ApiResponse.success(HttpStatus.OK, "hello world", "Hello, World!");
    }
}
