package com.ailene.lms.level;

import jakarta.validation.constraints.NotBlank;

public record LevelListRequest(@NotBlank String projectId) {
}
