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

import java.util.List;

@RestController
@RequestMapping("/api/v1/learnings")
@RequiredArgsConstructor
public class LearningsController {

    private final LearningsService learningsService;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping("/levels")
    public ResponseEntity<ApiResponse<List<LevelDto>>> levels(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody LevelListRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        List<LevelDto> response = learningsService.getLevels(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "levels retrieved successfully", response);
    }

    @PostMapping("/chapters")
    public ResponseEntity<ApiResponse<List<ChapterListItem>>> chapters(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody ChapterListRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        List<ChapterListItem> response = learningsService.getChapters(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "chapters retrieved successfully", response);
    }

    @PostMapping("/task")
    public ResponseEntity<ApiResponse<LearningsResponse>> tasks(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody LearningsRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        LearningsResponse response = learningsService.getTasks(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "learnings retrieved successfully", response);
    }

    @PostMapping("/today-focus")
    public ResponseEntity<ApiResponse<TodayFocusResponse>> todayFocus(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody TodayFocusRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        TodayFocusResponse response = learningsService.getTodayFocus(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "today focus retrieved successfully", response);
    }
}
