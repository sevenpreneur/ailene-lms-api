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
@RequestMapping("/api/v1/sponsor")
@RequiredArgsConstructor
public class SponsorController {

    private final SponsorService sponsorService;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping("/organization-stats")
    public ResponseEntity<ApiResponse<OrganizationStatsResponse>> organizationStats(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody SponsorProjectRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "organization stats retrieved successfully",
                sponsorService.getOrganizationStats(jwt, request));
    }

    @PostMapping("/executive-view")
    public ResponseEntity<ApiResponse<ExecutiveViewResponse>> executiveView(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody SponsorProjectRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "executive view retrieved successfully",
                sponsorService.getExecutiveView(jwt, request));
    }

    @PostMapping("/headline")
    public ResponseEntity<ApiResponse<HeadlineResponse>> headline(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody SponsorProjectRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "headline retrieved successfully",
                sponsorService.getHeadline(jwt, request));
    }

    @PostMapping("/program-health")
    public ResponseEntity<ApiResponse<ProgramHealthResponse>> programHealth(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody SponsorProjectRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "program health retrieved successfully",
                sponsorService.getProgramHealth(jwt, request));
    }

    @PostMapping("/recent-activity")
    public ResponseEntity<ApiResponse<RecentActivityResponse>> recentActivity(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody SponsorProjectRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "recent activity retrieved successfully",
                sponsorService.getRecentActivity(jwt, request));
    }

    @PostMapping("/weekly-trends")
    public ResponseEntity<ApiResponse<WeeklyTrendsResponse>> weeklyTrends(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody SponsorProjectRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "weekly trends retrieved successfully",
                sponsorService.getWeeklyTrends(jwt, request));
    }

    @PostMapping("/proficiency-trends")
    public ResponseEntity<ApiResponse<ProficiencyTrendsResponse>> proficiencyTrends(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody SponsorProjectRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "proficiency trends retrieved successfully",
                sponsorService.getProficiencyTrends(jwt, request));
    }

    @PostMapping("/level-distribution")
    public ResponseEntity<ApiResponse<LevelDistributionResponse>> levelDistribution(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody SponsorProjectRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "level distribution retrieved successfully",
                sponsorService.getLevelDistribution(jwt, request));
    }

    @PostMapping("/workforce-members")
    public ResponseEntity<ApiResponse<WorkforceResponse>> workforceMembers(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody SponsorProjectRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "workforce members retrieved successfully",
                sponsorService.getWorkforceMembers(jwt, request));
    }

    @PostMapping("/organization-leaderboard")
    public ResponseEntity<ApiResponse<OrganizationLeaderboardResponse>> organizationLeaderboard(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody SponsorProjectRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "organization leaderboard retrieved successfully",
                sponsorService.getOrganizationLeaderboard(jwt, request));
    }

    @PostMapping("/pre-assessment-organization")
    public ResponseEntity<ApiResponse<PreAssessmentOrganizationResponse>> preAssessmentOrganization(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody SponsorProjectRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "organization pre-assessment retrieved successfully",
                sponsorService.getPreAssessmentOrganization(jwt, request));
    }
}
