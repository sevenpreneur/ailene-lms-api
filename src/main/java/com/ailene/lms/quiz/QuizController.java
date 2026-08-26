package com.ailene.lms.quiz;

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
@RequestMapping("/api/v1/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping("/details")
    public ResponseEntity<ApiResponse<QuizDetailsResponse>> details(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody QuizDetailsRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        QuizDetailsResponse response = quizService.getQuizDetails(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "quiz retrieved successfully", response);
    }

    @PostMapping("/attempt")
    public ResponseEntity<ApiResponse<QuizAttemptResponse>> attempt(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody QuizAttemptRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        QuizAttemptResponse response = quizService.startAttempt(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "quiz attempt started", response);
    }

    @PostMapping("/update")
    public ResponseEntity<ApiResponse<QuizUpdateResponse>> update(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody QuizUpdateRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        QuizUpdateResponse response = quizService.saveDraft(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "quiz draft saved", response);
    }

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<QuizSubmitResponse>> submit(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody QuizSubmitRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        QuizSubmitResponse response = quizService.submit(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "quiz submitted successfully", response);
    }

    @PostMapping("/result")
    public ResponseEntity<ApiResponse<QuizResultResponse>> result(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody QuizResultRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        QuizResultResponse response = quizService.getResult(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "quiz result retrieved successfully", response);
    }

    @PostMapping("/auto-submit")
    public ResponseEntity<ApiResponse<Void>> autoSubmit(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody QuizAutoSubmitRequest request) {
        secretKeyGuard.requireValidSecretKey(authorization);
        quizService.autoSubmit(request.submissionId());
        return ApiResponse.success(HttpStatus.OK, "quiz auto-submitted", null);
    }
}
