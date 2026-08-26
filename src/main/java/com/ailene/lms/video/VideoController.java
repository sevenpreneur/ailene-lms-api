package com.ailene.lms.video;

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
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping("/details")
    public ResponseEntity<ApiResponse<VideoDetailsResponse>> details(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody VideoDetailsRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        VideoDetailsResponse response = videoService.getVideoDetails(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "video retrieved successfully", response);
    }

    @PostMapping("/completion")
    public ResponseEntity<ApiResponse<VideoCompletionResponse>> completion(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody VideoCompletionRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        VideoCompletionResponse response = videoService.completeVideo(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "video completed successfully", response);
    }
}
