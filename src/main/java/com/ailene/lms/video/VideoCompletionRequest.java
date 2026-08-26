package com.ailene.lms.video;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record VideoCompletionRequest(@NotNull @Positive Integer videoId) {
}
