package com.ailene.lms.preassessment;

import com.ailene.lms.auth.AuthService;
import com.ailene.lms.common.response.ApiResponse;
import com.ailene.lms.common.security.SecretKeyGuard;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pre-assessment")
@RequiredArgsConstructor
public class PreAssessmentController {

    private final PreAssessmentService preAssessmentService;
    private final AuthService authService;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<PreAssessmentCreateResponse>> create(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody PreAssessmentCreateRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        UUID userId = authService.resolveUserId(jwt);
        PreAssessmentCreateResponse response = preAssessmentService.create(userId, request);
        return ApiResponse.success(HttpStatus.CREATED, "pre-assessment submitted successfully", response);
    }

    @PostMapping("/score")
    public ResponseEntity<ApiResponse<PreAssessmentScoreResponse>> score(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody PreAssessmentProjectRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        UUID userId = authService.resolveUserId(jwt);
        PreAssessmentScoreResponse response = preAssessmentService.getScore(userId, request);
        return ApiResponse.success(HttpStatus.OK, "pre-assessment score retrieved successfully", response);
    }

    @PostMapping("/recommendations")
    public ResponseEntity<ApiResponse<PreAssessmentRecommendationsResponse>> recommendations(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody PreAssessmentProjectRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        UUID userId = authService.resolveUserId(jwt);
        PreAssessmentRecommendationsResponse response = preAssessmentService.getRecommendations(userId, request);
        return ApiResponse.success(HttpStatus.OK, "pre-assessment recommendations retrieved successfully", response);
    }

    @PostMapping("/recommendations/regenerate")
    public ResponseEntity<ApiResponse<PreAssessmentRecommendationsResponse>> regenerateRecommendations(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody PreAssessmentProjectRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        UUID userId = authService.resolveUserId(jwt);
        PreAssessmentRecommendationsResponse response = preAssessmentService.regenerateRecommendations(userId,
                request);
        return ApiResponse.success(HttpStatus.OK, "pre-assessment recommendations regeneration queued", response);
    }

    @PostMapping("/report-callback")
    public ResponseEntity<ApiResponse<Void>> reportCallback(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody PreAssessmentReportJobRequest request) {
        secretKeyGuard.requireValidSecretKey(authorization);
        preAssessmentService.generateReport(request.preAssessmentId());
        return ApiResponse.success(HttpStatus.OK, "pre-assessment report generated", null);
    }
}
