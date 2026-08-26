package com.ailene.lms.learnings;

import jakarta.validation.constraints.NotBlank;

public record LevelListRequest(@NotBlank String projectId) {
}
