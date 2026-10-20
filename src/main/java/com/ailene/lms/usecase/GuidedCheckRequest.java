package com.ailene.lms.usecase;

import jakarta.validation.constraints.*;

public record GuidedCheckRequest(@NotNull @Positive Integer sessionId, @NotNull @Positive Integer stepId,
        @NotNull Boolean isDone, @Size(max = 2000) String note, @Size(max = 2000) String skippedReason) {
}
