package com.ailene.lms.material;

import jakarta.validation.constraints.NotBlank;

public record LevelMaterialsRequest(@NotBlank String materialId) {
}
