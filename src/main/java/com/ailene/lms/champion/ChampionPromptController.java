package com.ailene.lms.champion;

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
@RequestMapping("/api/v1/champion/prompts")
@RequiredArgsConstructor
public class ChampionPromptController {

    private final ChampionAssignmentService championAssignmentService;
    private final ChampionReviewService championReviewService;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping("/assign")
    public ResponseEntity<ApiResponse<AssignmentResult>> assign(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody AssignLibraryRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "prompt assigned successfully",
                championAssignmentService.assignPrompt(jwt, request));
    }

    @PostMapping("/create-assignment")
    public ResponseEntity<ApiResponse<CreatePromptAssignmentResponse>> createAssignment(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody CreatePromptAssignmentRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "prompt assignment created successfully",
                championAssignmentService.createPromptAssignment(jwt, request));
    }

    @PostMapping("/submissions")
    public ResponseEntity<ApiResponse<ReviewQueueResponse>> submissions(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody ChampionProjectRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "prompt submissions retrieved successfully",
                championReviewService.getPromptQueue(jwt, request));
    }

    @PostMapping("/submission-details")
    public ResponseEntity<ApiResponse<PromptSubmissionDetail>> submissionDetails(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody SubmissionDetailRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "prompt submission details retrieved successfully",
                championReviewService.getPromptDetail(jwt, request));
    }

    @PostMapping("/review")
    public ResponseEntity<ApiResponse<ReviewResult>> review(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody ReviewPromptRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "review saved successfully",
                championReviewService.reviewPrompt(jwt, request));
    }
}
