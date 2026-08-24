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
@RequestMapping("/api/use-cases")
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
}
