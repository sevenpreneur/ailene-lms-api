package com.ailene.lms.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record NotificationReadRequest(@NotBlank String projectId, @NotEmpty List<Integer> notificationIds) {
}
