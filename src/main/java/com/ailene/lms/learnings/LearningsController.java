package com.ailene.lms.learnings;

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
@RequestMapping("/api/learnings")
@RequiredArgsConstructor
public class LearningsController {

    private final LearningsService learningsService;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping
    public ResponseEntity<ApiResponse<LearningsResponse>> tasks(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody LearningsRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        LearningsResponse response = learningsService.getTasks(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "learnings retrieved successfully", response);
    }

    @PostMapping("/material-details")
    public ResponseEntity<ApiResponse<MaterialDetailsResponse>> materialDetails(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody MaterialDetailsRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        MaterialDetailsResponse response = learningsService.getMaterialDetails(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "material retrieved successfully", response);
    }

    @PostMapping("/video-details")
    public ResponseEntity<ApiResponse<VideoDetailsResponse>> videoDetails(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody VideoDetailsRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        VideoDetailsResponse response = learningsService.getVideoDetails(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "video retrieved successfully", response);
    }

    @PostMapping("/quiz-details")
    public ResponseEntity<ApiResponse<QuizDetailsResponse>> quizDetails(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody QuizDetailsRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        QuizDetailsResponse response = learningsService.getQuizDetails(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "quiz retrieved successfully", response);
    }

    @PostMapping("/material-completion")
    public ResponseEntity<ApiResponse<MaterialCompletionResponse>> materialCompletion(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody MaterialCompletionRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        MaterialCompletionResponse response = learningsService.completeMaterial(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "material completed successfully", response);
    }

    @PostMapping("/video-completion")
    public ResponseEntity<ApiResponse<VideoCompletionResponse>> videoCompletion(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody VideoCompletionRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        VideoCompletionResponse response = learningsService.completeVideo(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "video completed successfully", response);
    }
}
