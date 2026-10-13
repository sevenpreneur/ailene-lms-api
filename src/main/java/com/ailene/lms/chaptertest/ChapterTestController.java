package com.ailene.lms.chaptertest;

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
@RequestMapping("/api/v1/chapter-tests")
@RequiredArgsConstructor
public class ChapterTestController {

    private final ChapterTestService chapterTestService;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping("/details")
    public ResponseEntity<ApiResponse<ChapterTestDetailsResponse>> details(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody ChapterTestDetailsRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        ChapterTestDetailsResponse response = chapterTestService.getDetails(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "chapter test retrieved successfully", response);
    }

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<ChapterTestSubmitResponse>> submit(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody ChapterTestSubmitRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        ChapterTestSubmitResponse response = chapterTestService.submit(jwt, request);
        return ApiResponse.success(HttpStatus.CREATED, "chapter test submitted successfully", response);
    }

    @PostMapping("/result")
    public ResponseEntity<ApiResponse<ChapterTestResultResponse>> result(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody ChapterTestResultRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        ChapterTestResultResponse response = chapterTestService.getResult(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "chapter test result retrieved successfully", response);
    }
}
