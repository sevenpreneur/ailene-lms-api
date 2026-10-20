package com.ailene.lms.usecase;

import com.ailene.lms.auth.AuthService;
import com.ailene.lms.common.response.ApiResponse;
import com.ailene.lms.common.security.SecretKeyGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/use-cases/guided")
@RequiredArgsConstructor
public class GuidedUseCaseController {
    private final GuidedUseCaseStore store;
    private final GuidedUseCaseService service;
    private final AuthService auth;
    private final SecretKeyGuard bearer;

    @PostMapping("/details")
    public ResponseEntity<ApiResponse<GuidedSessionResponse>> details(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody GuidedDetailsRequest request) {
        return ApiResponse.success(HttpStatus.OK, "guided session retrieved successfully", store.details(user(authorization), request));
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<GuidedSessionResponse>> save(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody GuidedSaveRequest request) {
        return ApiResponse.success(HttpStatus.OK, "guided draft saved successfully", store.save(user(authorization), request));
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<GuidedSessionResponse>> generate(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody GuidedGenerateRequest request) {
        return ApiResponse.success(HttpStatus.OK, "guided workflow generated successfully", service.generate(user(authorization), request));
    }

    @PostMapping("/check")
    public ResponseEntity<ApiResponse<GuidedStep>> check(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody GuidedCheckRequest request) {
        return ApiResponse.success(HttpStatus.OK, "guided step saved successfully", store.check(user(authorization), request));
    }

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<GuidedSubmitResponse>> submit(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody GuidedSubmitRequest request) {
        return ApiResponse.success(HttpStatus.OK, "guided use case submitted successfully", store.submit(user(authorization), request));
    }

    private UUID user(String authorization) {
        return auth.resolveUserId(bearer.extractBearerToken(authorization));
    }
}
