package com.ailene.lms.material;

import jakarta.validation.constraints.NotBlank;

public record MaterialCompletionRequest(@NotBlank String materialId) {
}
