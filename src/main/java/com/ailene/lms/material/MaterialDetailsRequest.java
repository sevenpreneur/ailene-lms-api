package com.ailene.lms.material;

import jakarta.validation.constraints.NotBlank;

public record MaterialDetailsRequest(@NotBlank String materialId) {
}
