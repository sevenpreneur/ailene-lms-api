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
@RequestMapping("/api/v1/champion/use-cases")
@RequiredArgsConstructor
public class ChampionUseCaseController {

    private final ChampionAssignmentService championAssignmentService;
    private final ChampionReviewService championReviewService;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping("/assign")
    public ResponseEntity<ApiResponse<AssignmentResult>> assign(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody AssignLibraryRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "use case assigned successfully",
                championAssignmentService.assignUseCase(jwt, request));
    }

    @PostMapping("/create-assignment")
    public ResponseEntity<ApiResponse<CreateUseCaseAssignmentResponse>> createAssignment(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody CreateUseCaseAssignmentRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "use case assignment created successfully",
                championAssignmentService.createUseCaseAssignment(jwt, request));
    }

    @PostMapping("/submissions")
    public ResponseEntity<ApiResponse<ReviewQueueResponse>> submissions(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody ChampionProjectRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "use case submissions retrieved successfully",
                championReviewService.getUseCaseQueue(jwt, request));
    }

    @PostMapping("/submission-details")
    public ResponseEntity<ApiResponse<UseCaseSubmissionDetail>> submissionDetails(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody SubmissionDetailRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "use case submission details retrieved successfully",
                championReviewService.getUseCaseDetail(jwt, request));
    }

    @PostMapping("/review")
    public ResponseEntity<ApiResponse<ReviewResult>> review(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody ReviewUseCaseRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "review saved successfully",
                championReviewService.reviewUseCase(jwt, request));
    }
}
