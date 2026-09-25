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
@RequestMapping("/api/v1/admin/projects")
@RequiredArgsConstructor
public class AdminProjectController {

    private final AdminGroupService adminGroupService;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping
    public ResponseEntity<ApiResponse<List<AdminProjectDto>>> list(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        String token = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "projects retrieved successfully", adminGroupService.listProjects(token));
    }
}
