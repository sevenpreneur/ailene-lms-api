package com.ailene.lms.student;

import jakarta.validation.constraints.NotBlank;

public record LevelListRequest(@NotBlank String projectId) {
}
