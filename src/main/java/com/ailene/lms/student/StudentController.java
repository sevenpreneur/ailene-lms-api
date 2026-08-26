package com.ailene.lms.student;

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
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping("/status")
    public ResponseEntity<ApiResponse<StudentStatusResponse>> status(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody StudentStatusRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        StudentStatusResponse response = studentService.getStatus(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "student status retrieved successfully", response);
    }

    @PostMapping("/level-progress")
    public ResponseEntity<ApiResponse<LevelProgressResponse>> levelProgress(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody LevelProgressRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        LevelProgressResponse response = studentService.getLevelProgress(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "level progress retrieved successfully", response);
    }

    @PostMapping("/competency")
    public ResponseEntity<ApiResponse<CompetencyProfileResponse>> competency(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody CompetencyRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        CompetencyProfileResponse response = studentService.getCompetencyProfile(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "competency profile retrieved successfully", response);
    }

    @PostMapping("/leaderboard")
    public ResponseEntity<ApiResponse<LeaderboardResponse>> leaderboard(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody LeaderboardRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        LeaderboardResponse response = studentService.getLeaderboard(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "leaderboard retrieved successfully", response);
    }
}
