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
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminMemberService adminMemberService;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping
    public ResponseEntity<ApiResponse<List<AdminMemberDto>>> list(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody ListMembersRequest request) {
        String token = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "users retrieved successfully", adminMemberService.listMembers(token, request));
    }

    @PostMapping("/invite")
    public ResponseEntity<ApiResponse<InviteMemberResponse>> invite(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody InviteMemberRequest request) {
        String token = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.CREATED, "user invited successfully", adminMemberService.inviteMember(token, request));
    }

    @PostMapping("/update")
    public ResponseEntity<ApiResponse<AdminMemberDto>> update(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody UpdateMemberRequest request) {
        String token = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "user updated successfully", adminMemberService.updateMember(token, request));
    }

    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<AdminDeleteResponse>> delete(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody DeleteMemberRequest request) {
        String token = secretKeyGuard.extractBearerToken(authorization);
        return ApiResponse.success(HttpStatus.OK, "user removed from project successfully", adminMemberService.deleteMember(token, request));
    }
}
