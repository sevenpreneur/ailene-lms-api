package com.ailene.lms.announcement;

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
@RequestMapping("/api/v1/announcement")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping("/details")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> details(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody AnnouncementDetailsRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        AnnouncementResponse response = announcementService.getDetails(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "announcement retrieved successfully", response);
    }

    @PostMapping("/upsert")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> upsert(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody AnnouncementUpsertRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        AnnouncementResponse response = announcementService.upsert(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "announcement upserted successfully", response);
    }
}
