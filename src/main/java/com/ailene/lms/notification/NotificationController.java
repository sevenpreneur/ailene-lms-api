package com.ailene.lms.notification;

import com.ailene.lms.auth.AuthService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthService authService;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationListResponse>> list(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody NotificationListRequest request) {
        UUID userId = authService.resolveUserId(secretKeyGuard.extractBearerToken(authorization));
        NotificationListResponse response = notificationService.list(userId, request);
        return ApiResponse.success(HttpStatus.OK, "notifications retrieved successfully", response);
    }

    @PostMapping("/unread-count")
    public ResponseEntity<ApiResponse<NotificationUnreadResponse>> unreadCount(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody NotificationProjectRequest request) {
        UUID userId = authService.resolveUserId(secretKeyGuard.extractBearerToken(authorization));
        NotificationUnreadResponse response = notificationService.unreadCount(userId, request);
        return ApiResponse.success(HttpStatus.OK, "unread count retrieved successfully", response);
    }

    @PostMapping("/read")
    public ResponseEntity<ApiResponse<NotificationReadResponse>> read(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody NotificationReadRequest request) {
        UUID userId = authService.resolveUserId(secretKeyGuard.extractBearerToken(authorization));
        NotificationReadResponse response = notificationService.markRead(userId, request);
        return ApiResponse.success(HttpStatus.OK, "notifications marked as read", response);
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<NotificationReadResponse>> readAll(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody NotificationProjectRequest request) {
        UUID userId = authService.resolveUserId(secretKeyGuard.extractBearerToken(authorization));
        NotificationReadResponse response = notificationService.markAllRead(userId, request);
        return ApiResponse.success(HttpStatus.OK, "all notifications marked as read", response);
    }
}
