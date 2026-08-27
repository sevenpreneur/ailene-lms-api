package com.ailene.lms.coaching;

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
@RequestMapping("/api/v1/coaching-notes")
@RequiredArgsConstructor
public class CoachingNoteController {

    private final CoachingNoteService coachingNoteService;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping
    public ResponseEntity<ApiResponse<List<CoachingNoteItem>>> list(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody CoachingNotesRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        List<CoachingNoteItem> response = coachingNoteService.list(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "coaching notes retrieved successfully", response);
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<CoachingNoteItem>> create(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody CoachingNoteCreateRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        CoachingNoteItem response = coachingNoteService.create(jwt, request);
        return ApiResponse.success(HttpStatus.CREATED, "coaching note created successfully", response);
    }
}
