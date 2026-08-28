package com.ailene.lms.usecase;

import com.ailene.lms.auth.AuthService;
import com.ailene.lms.common.pagination.PagedResponse;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/use-cases")
@RequiredArgsConstructor
public class UseCaseController {

    private final UseCaseService useCaseService;
    private final AuthService authService;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping
    public ResponseEntity<ApiResponse<PagedResponse<UseCaseListItem>>> list(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody UseCaseListRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        UUID userId = authService.resolveUserId(jwt);
        PagedResponse<UseCaseListItem> response = useCaseService.list(userId, request);
        return ApiResponse.success(HttpStatus.OK, "use cases retrieved successfully", response);
    }

    @PostMapping("/assigned")
    public ResponseEntity<ApiResponse<List<UseCaseAssignedItem>>> assigned(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody UseCaseAssignedRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        UUID userId = authService.resolveUserId(jwt);
        List<UseCaseAssignedItem> response = useCaseService.listAssigned(userId, request);
        return ApiResponse.success(HttpStatus.OK, "assigned use cases retrieved successfully", response);
    }

    @PostMapping("/details")
    public ResponseEntity<ApiResponse<UseCaseDetailsResponse>> details(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody UseCaseDetailsRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        UUID userId = authService.resolveUserId(jwt);
        UseCaseDetailsResponse response = useCaseService.getDetails(userId, request);
        return ApiResponse.success(HttpStatus.OK, "use case retrieved successfully", response);
    }

    @PostMapping("/self-create")
    public ResponseEntity<ApiResponse<UseCaseDetailsResponse>> selfCreate(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody UseCaseSelfCreateRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        UUID userId = authService.resolveUserId(jwt);
        UseCaseDetailsResponse response = useCaseService.selfCreate(userId, request);
        return ApiResponse.success(HttpStatus.CREATED, "self-created use case submitted successfully", response);
    }

    @PostMapping("/self-assign")
    public ResponseEntity<ApiResponse<UseCaseDetailsResponse>> selfAssign(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody UseCaseSelfAssignRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        UUID userId = authService.resolveUserId(jwt);
        UseCaseDetailsResponse response = useCaseService.selfAssign(userId, request);
        return ApiResponse.success(HttpStatus.OK, "use case self-assigned successfully", response);
    }

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<UseCaseDetailsResponse>> submit(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody UseCaseSubmitRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        UUID userId = authService.resolveUserId(jwt);
        UseCaseDetailsResponse response = useCaseService.submit(userId, request);
        return ApiResponse.success(HttpStatus.OK, "use case submitted successfully", response);
    }
}
