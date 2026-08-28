package com.ailene.lms.preassessment;

import jakarta.validation.constraints.NotBlank;

public record PreAssessmentProjectRequest(@NotBlank String projectId) {
}
