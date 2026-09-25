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
@RequestMapping("/api/v1/admin/groups")
@RequiredArgsConstructor
public class AdminGroupController {

    private final AdminGroupService adminGroupService;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping
    public ResponseEntity<ApiResponse<List<AdminGroupDto>>> list(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody AdminProjectRequest request) {
        String token = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "groups retrieved successfully", adminGroupService.listGroups(token, request));
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<AdminGroupDto>> create(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody CreateGroupRequest request) {
        String token = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.CREATED, "group created successfully", adminGroupService.createGroup(token, request));
    }

    @PostMapping("/update")
    public ResponseEntity<ApiResponse<AdminGroupDto>> update(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody UpdateGroupRequest request) {
        String token = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "group updated successfully", adminGroupService.updateGroup(token, request));
    }

    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<AdminDeleteResponse>> delete(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody DeleteGroupRequest request) {
        String token = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "group deleted successfully", adminGroupService.deleteGroup(token, request));
    }
}
