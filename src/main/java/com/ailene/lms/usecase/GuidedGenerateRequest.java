package com.ailene.lms.usecase;

import jakarta.validation.constraints.*;

public record GuidedGenerateRequest(@NotNull @Positive Integer sessionId, @Size(max = 2000) String feedback) {
}
