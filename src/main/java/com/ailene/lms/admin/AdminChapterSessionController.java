package com.ailene.lms.admin;

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
@RequestMapping("/api/v1/admin/chapter-sessions")
@RequiredArgsConstructor
public class AdminChapterSessionController {

    private final AdminChapterSessionService adminChapterSessionService;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping
    public ResponseEntity<ApiResponse<List<AdminChapterSessionDto>>> list(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody AdminProjectRequest request) {
        String token = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "chapter sessions retrieved successfully",
                adminChapterSessionService.listSessions(token, request));
    }

    @PostMapping("/lock")
    public ResponseEntity<ApiResponse<AdminChapterSessionDto>> lock(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody UpdateChapterSessionLockRequest request) {
        String token = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "chapter session updated successfully",
                adminChapterSessionService.updateLock(token, request));
    }
}
