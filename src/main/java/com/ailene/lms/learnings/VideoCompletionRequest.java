package com.ailene.lms.learnings;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record VideoCompletionRequest(@NotNull @Positive Integer videoId) {
}
