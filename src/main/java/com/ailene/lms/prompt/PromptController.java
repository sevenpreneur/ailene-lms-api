package com.ailene.lms.prompt;

import com.ailene.lms.auth.AuthService;
import com.ailene.lms.common.pagination.PagedResponse;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prompts")
@RequiredArgsConstructor
public class PromptController {

    private final PromptService promptService;
    private final PromptEvaluationService promptEvaluationService;
    private final AuthService authService;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping
    public ResponseEntity<ApiResponse<PagedResponse<PromptListItem>>> list(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody PromptListRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        UUID userId = authService.resolveUserId(jwt);
        PagedResponse<PromptListItem> response = promptService.list(userId, request);
        return ApiResponse.success(HttpStatus.OK, "prompts retrieved successfully", response);
    }

    @PostMapping("/assigned")
    public ResponseEntity<ApiResponse<List<PromptAssignedItem>>> assigned(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody PromptAssignedRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        UUID userId = authService.resolveUserId(jwt);
        List<PromptAssignedItem> response = promptService.listAssigned(userId, request);
        return ApiResponse.success(HttpStatus.OK, "assigned prompts retrieved successfully", response);
    }

    @PostMapping("/details")
    public ResponseEntity<ApiResponse<PromptDetailsResponse>> details(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody PromptDetailsRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        UUID userId = authService.resolveUserId(jwt);
        PromptDetailsResponse response = promptService.getDetails(userId, request);
        return ApiResponse.success(HttpStatus.OK, "prompt retrieved successfully", response);
    }

    @PostMapping("/self-create")
    public ResponseEntity<ApiResponse<PromptDetailsResponse>> selfCreate(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody PromptSelfCreateRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        UUID userId = authService.resolveUserId(jwt);
        PromptDetailsResponse response = promptService.selfCreate(userId, request);
        return ApiResponse.success(HttpStatus.CREATED, "self-created prompt submitted successfully", response);
    }

    @PostMapping("/self-assign")
    public ResponseEntity<ApiResponse<PromptDetailsResponse>> selfAssign(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody PromptSelfAssignRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        UUID userId = authService.resolveUserId(jwt);
        PromptDetailsResponse response = promptService.selfAssign(userId, request);
        return ApiResponse.success(HttpStatus.OK, "prompt self-assigned successfully", response);
    }

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<PromptDetailsResponse>> submit(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody PromptSubmitRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        UUID userId = authService.resolveUserId(jwt);
        PromptDetailsResponse response = promptService.submit(userId, request);
        return ApiResponse.success(HttpStatus.OK, "prompt submitted successfully", response);
    }

    @PostMapping("/evaluate-callback")
    public ResponseEntity<ApiResponse<Void>> evaluateCallback(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody PromptEvaluationJobRequest request) {
        secretKeyGuard.requireValidSecretKey(authorization);
        promptEvaluationService.evaluate(request);
        return ApiResponse.success(HttpStatus.OK, "prompt evaluation processed", null);
    }
}
