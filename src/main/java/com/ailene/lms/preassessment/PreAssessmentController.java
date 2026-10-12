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

    private final PreAssessmentAttemptService attemptService;
    private final AuthService authService;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping("/questions")
    public ResponseEntity<ApiResponse<PreAssessmentQuestionsResponse>> questions(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody PreAssessmentProjectRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        UUID userId = authService.resolveUserId(jwt);
        PreAssessmentQuestionsResponse response = attemptService.getQuestions(userId, request);
        return ApiResponse.success(HttpStatus.OK, "pre-assessment questions retrieved successfully", response);
    }

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<PreAssessmentSubmitResponse>> submit(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody PreAssessmentSubmitRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        UUID userId = authService.resolveUserId(jwt);
        PreAssessmentSubmitResponse response = attemptService.submit(userId, request);
        return ApiResponse.success(HttpStatus.CREATED, "pre-assessment answers submitted successfully", response);
    }

    @PostMapping("/score")
    public ResponseEntity<ApiResponse<PreAssessmentScoreResponse>> score(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody PreAssessmentProjectRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        UUID userId = authService.resolveUserId(jwt);
        PreAssessmentScoreResponse response = attemptService.getScore(userId, request);
        return ApiResponse.success(HttpStatus.OK, "pre-assessment score retrieved successfully", response);
    }
}
