package com.ailene.lms.notification;

import jakarta.validation.constraints.NotBlank;

public record NotificationListRequest(@NotBlank String projectId, Boolean unreadOnly, Integer page,
        Integer pageSize) {
}
