package com.ailene.lms.learnings;

import jakarta.validation.constraints.NotBlank;

public record MaterialDetailsRequest(@NotBlank String materialId) {
}
