package com.ailene.lms.student;

import jakarta.validation.constraints.NotBlank;

public record CompetencyRequest(@NotBlank String projectId) {
}
