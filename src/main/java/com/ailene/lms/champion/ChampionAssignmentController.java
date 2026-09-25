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
@RequestMapping("/api/v1/champion/assignments")
@RequiredArgsConstructor
public class ChampionAssignmentController {

    private final ChampionDraftService championDraftService;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<AssignmentDraftBatch>> generate(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody GenerateAssignmentRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "assignment drafts generated successfully",
                championDraftService.generate(jwt, request));
    }

    @PostMapping("/drafts")
    public ResponseEntity<ApiResponse<AssignmentDraftBatchesResponse>> drafts(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody ChampionProjectRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "assignment drafts retrieved successfully",
                championDraftService.listBatches(jwt, request));
    }

    @PostMapping("/drafts/delete")
    public ResponseEntity<ApiResponse<DeleteDraftResponse>> deleteDraft(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody DeleteDraftRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "assignment draft deleted successfully",
                championDraftService.deleteDraft(jwt, request));
    }
}
