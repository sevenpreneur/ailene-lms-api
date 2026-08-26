package com.ailene.lms.student;

import jakarta.validation.constraints.NotBlank;

public record LevelProgressRequest(@NotBlank String projectId) {
}
