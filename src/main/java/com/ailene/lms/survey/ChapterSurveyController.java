package com.ailene.lms.survey;

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
@RequestMapping("/api/v1/chapter-surveys")
@RequiredArgsConstructor
public class ChapterSurveyController {

    private final ChapterSurveyService chapterSurveyService;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping("/details")
    public ResponseEntity<ApiResponse<ChapterSurveyDetailsResponse>> details(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody ChapterSurveyDetailsRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        ChapterSurveyDetailsResponse response = chapterSurveyService.getDetails(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "chapter survey retrieved successfully", response);
    }

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<ChapterSurveyAnswer>> submit(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody ChapterSurveySubmitRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        ChapterSurveyAnswer response = chapterSurveyService.submit(jwt, request);
        return ApiResponse.success(HttpStatus.CREATED, "chapter survey submitted successfully", response);
    }
}
