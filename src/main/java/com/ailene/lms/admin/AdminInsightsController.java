package com.ailene.lms.admin;

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

@RestController
@RequestMapping("/api/v1/admin/insights")
@RequiredArgsConstructor
public class AdminInsightsController {

    private final AdminInsightsService service;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping("/overview")
    public ResponseEntity<ApiResponse<AdminInsightsOverview>> overview(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody AdminInsightsRequest r) {
        return ApiResponse.success(HttpStatus.OK, "insights overview retrieved successfully",
                service.overview(token(authorization), r));
    }

    @PostMapping("/pre-assessment")
    public ResponseEntity<ApiResponse<AdminPreAssessmentInsights>> preAssessment(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody AdminInsightsRequest r) {
        return ApiResponse.success(HttpStatus.OK, "pre-assessment insights retrieved successfully",
                service.preAssessment(token(authorization), r));
    }

    @PostMapping("/guided-use-cases")
    public ResponseEntity<ApiResponse<AdminGuidedInsights>> guidedUseCases(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody AdminInsightsRequest r) {
        return ApiResponse.success(HttpStatus.OK, "guided use case insights retrieved successfully",
                service.guidedUseCases(token(authorization), r));
    }

    @PostMapping("/session-reviews")
    public ResponseEntity<ApiResponse<AdminReviewInsights>> sessionReviews(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody AdminInsightsRequest r) {
        return ApiResponse.success(HttpStatus.OK, "session review insights retrieved successfully",
                service.sessionReviews(token(authorization), r));
    }

    @PostMapping("/chapter-tests")
    public ResponseEntity<ApiResponse<AdminTestInsights>> chapterTests(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody AdminInsightsRequest r) {
        return ApiResponse.success(HttpStatus.OK, "chapter test insights retrieved successfully",
                service.chapterTests(token(authorization), r));
    }

    @PostMapping("/quizzes")
    public ResponseEntity<ApiResponse<AdminQuizInsights>> quizzes(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody AdminInsightsRequest r) {
        return ApiResponse.success(HttpStatus.OK, "quiz insights retrieved successfully",
                service.quizzes(token(authorization), r));
    }

    private String token(String authorization) {
        return secretKeyGuard.extractBearerToken(authorization);
    }
}
