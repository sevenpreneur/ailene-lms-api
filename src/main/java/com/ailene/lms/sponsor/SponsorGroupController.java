package com.ailene.lms.sponsor;

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
@RequestMapping("/api/v1/sponsor/groups")
@RequiredArgsConstructor
public class SponsorGroupController {

    private final SponsorGroupService sponsorGroupService;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping("/departments")
    public ResponseEntity<ApiResponse<DepartmentListResponse>> departments(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody SponsorProjectRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "departments retrieved successfully",
                sponsorGroupService.getDepartments(jwt, request));
    }

    @PostMapping("/overview")
    public ResponseEntity<ApiResponse<GroupOverviewResponse>> overview(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody SponsorGroupRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "group overview retrieved successfully",
                sponsorGroupService.getOverview(jwt, request));
    }

    @PostMapping("/level-distribution")
    public ResponseEntity<ApiResponse<GroupLevelDistributionResponse>> levelDistribution(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody SponsorGroupRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "group level distribution retrieved successfully",
                sponsorGroupService.getLevelDistribution(jwt, request));
    }

    @PostMapping("/top-use-cases")
    public ResponseEntity<ApiResponse<GroupTopUseCasesResponse>> topUseCases(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody SponsorGroupRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "group top use cases retrieved successfully",
                sponsorGroupService.getTopUseCases(jwt, request));
    }

    @PostMapping("/attention-members")
    public ResponseEntity<ApiResponse<AttentionMembersResponse>> attentionMembers(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody SponsorGroupRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "group attention members retrieved successfully",
                sponsorGroupService.getAttentionMembers(jwt, request));
    }
}
