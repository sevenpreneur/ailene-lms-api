package com.ailene.lms.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateChapterSessionLockRequest(@NotBlank String projectId, @NotNull Integer sessionId,
        @NotNull Boolean isLocked) {
}
