package com.ailene.lms.notification;

import jakarta.validation.constraints.NotBlank;

public record NotificationProjectRequest(@NotBlank String projectId) {
}
