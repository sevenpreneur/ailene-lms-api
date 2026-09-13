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
@RequestMapping("/api/v1/champion")
@RequiredArgsConstructor
public class ChampionController {

    private final ChampionTeamService championTeamService;
    private final ChampionBaselineService championBaselineService;
    private final ChampionReportService championReportService;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping("/members")
    public ResponseEntity<ApiResponse<ChampionMembersResponse>> members(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody ChampionMembersRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "team members retrieved successfully",
                championTeamService.getMembers(jwt, request));
    }

    @PostMapping("/member-details")
    public ResponseEntity<ApiResponse<MemberDetailResponse>> memberDetails(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody MemberDetailRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "member details retrieved successfully",
                championTeamService.getMemberDetail(jwt, request));
    }

    @PostMapping("/pre-assessment-team")
    public ResponseEntity<ApiResponse<PreAssessmentTeamResponse>> preAssessmentTeam(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody ChampionProjectRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "team pre-assessment retrieved successfully",
                championBaselineService.getTeamBaseline(jwt, request));
    }

    @PostMapping("/report")
    public ResponseEntity<ApiResponse<ChampionReportResponse>> report(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody ChampionReportRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "champion report retrieved successfully",
                championReportService.getReport(jwt, request));
    }
}
