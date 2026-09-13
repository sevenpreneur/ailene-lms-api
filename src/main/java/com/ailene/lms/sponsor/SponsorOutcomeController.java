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
@RequestMapping("/api/v1/sponsor/outcome")
@RequiredArgsConstructor
public class SponsorOutcomeController {

    private final SponsorOutcomeService sponsorOutcomeService;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping("/overview")
    public ResponseEntity<ApiResponse<OutcomeOverviewResponse>> overview(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody SponsorProjectRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "outcome overview retrieved successfully",
                sponsorOutcomeService.getOverview(jwt, request));
    }

    @PostMapping("/level-distribution")
    public ResponseEntity<ApiResponse<OutcomeLevelDistributionResponse>> levelDistribution(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody SponsorProjectRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "outcome level distribution retrieved successfully",
                sponsorOutcomeService.getLevelDistribution(jwt, request));
    }

    @PostMapping("/roi-trend")
    public ResponseEntity<ApiResponse<RoiTrendResponse>> roiTrend(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody SponsorProjectRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "roi trend retrieved successfully",
                sponsorOutcomeService.getRoiTrend(jwt, request));
    }

    @PostMapping("/department-roi")
    public ResponseEntity<ApiResponse<DepartmentRoiResponse>> departmentRoi(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody SponsorProjectRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "department roi retrieved successfully",
                sponsorOutcomeService.getDepartmentRoi(jwt, request));
    }

    @PostMapping("/top-performers")
    public ResponseEntity<ApiResponse<TopPerformersResponse>> topPerformers(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody SponsorProjectRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "top performers retrieved successfully",
                sponsorOutcomeService.getTopPerformers(jwt, request));
    }
}
