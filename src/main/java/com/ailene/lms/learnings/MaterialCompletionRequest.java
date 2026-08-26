package com.ailene.lms.learnings;

import jakarta.validation.constraints.NotBlank;

public record MaterialCompletionRequest(@NotBlank String materialId) {
}
