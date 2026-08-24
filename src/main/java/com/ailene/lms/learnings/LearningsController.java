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

@RestController
@RequestMapping("/api/learnings")
@RequiredArgsConstructor
public class LearningsController {

    private final LearningsService learningsService;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping
    public ResponseEntity<ApiResponse<LearningsResponse>> tasks(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody LearningsRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        LearningsResponse response = learningsService.getTasks(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "learnings retrieved successfully", response);
    }
}
