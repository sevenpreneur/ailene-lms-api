package com.ailene.lms.learnings;

import jakarta.validation.constraints.NotBlank;

public record LevelMaterialsRequest(@NotBlank String materialId) {
}
