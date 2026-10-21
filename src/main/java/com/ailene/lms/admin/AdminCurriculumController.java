package com.ailene.lms.admin;

import com.ailene.lms.common.response.ApiResponse;
import com.ailene.lms.common.security.SecretKeyGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/curriculum")
@RequiredArgsConstructor
public class AdminCurriculumController {
    private final AdminCurriculumService service;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping
    public ResponseEntity<ApiResponse<AdminCurriculumDto>> list(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody AdminProjectRequest r) {
        return ApiResponse.success(HttpStatus.OK, "curriculum retrieved successfully", service.list(token(authorization), r));
    }

    @PostMapping("/chapters/create")
    public ResponseEntity<ApiResponse<AdminCurriculumService.IdResponse>> createChapter(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody AdminCurriculumRequest.Chapter r) {
        return ApiResponse.success(HttpStatus.CREATED, "session created successfully", service.saveChapter(token(authorization), r, true));
    }

    @PostMapping("/chapters/update")
    public ResponseEntity<ApiResponse<AdminCurriculumService.IdResponse>> updateChapter(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody AdminCurriculumRequest.Chapter r) {
        return ApiResponse.success(HttpStatus.OK, "session updated successfully", service.saveChapter(token(authorization), r, false));
    }

    @PostMapping("/chapters/delete")
    public ResponseEntity<ApiResponse<AdminDeleteResponse>> deleteChapter(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody AdminCurriculumRequest.ChapterRef r) {
        return ApiResponse.success(HttpStatus.OK, "session deleted successfully", service.deleteChapter(token(authorization), r));
    }

    @PostMapping("/schedules/create")
    public ResponseEntity<ApiResponse<AdminCurriculumService.IdResponse>> createSession(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody AdminCurriculumRequest.Session r) {
        return ApiResponse.success(HttpStatus.CREATED, "schedule created successfully", service.saveSession(token(authorization), r, true));
    }

    @PostMapping("/schedules/update")
    public ResponseEntity<ApiResponse<AdminCurriculumService.IdResponse>> updateSession(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody AdminCurriculumRequest.Session r) {
        return ApiResponse.success(HttpStatus.OK, "schedule updated successfully", service.saveSession(token(authorization), r, false));
    }

    @PostMapping("/schedules/delete")
    public ResponseEntity<ApiResponse<AdminDeleteResponse>> deleteSession(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody AdminCurriculumRequest.SessionRef r) {
        return ApiResponse.success(HttpStatus.OK, "schedule deleted successfully", service.deleteSession(token(authorization), r));
    }

    @PostMapping("/contents/create")
    public ResponseEntity<ApiResponse<AdminCurriculumService.SavedResponse>> createContent(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody AdminCurriculumRequest.Content r) {
        return ApiResponse.success(HttpStatus.CREATED, "content created successfully", service.saveContent(token(authorization), r, true));
    }

    @PostMapping("/contents/update")
    public ResponseEntity<ApiResponse<AdminCurriculumService.SavedResponse>> updateContent(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody AdminCurriculumRequest.Content r) {
        return ApiResponse.success(HttpStatus.OK, "content updated successfully", service.saveContent(token(authorization), r, false));
    }

    @PostMapping("/contents/delete")
    public ResponseEntity<ApiResponse<AdminDeleteResponse>> deleteContent(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody AdminCurriculumRequest.ContentRef r) {
        return ApiResponse.success(HttpStatus.OK, "content deleted successfully", service.deleteContent(token(authorization), r));
    }

    private String token(String authorization) { return secretKeyGuard.extractBearerToken(authorization); }
}
