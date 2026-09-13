package com.ailene.lms.champion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SubmissionDetailRequest(@NotBlank String projectId, @NotNull @Positive Integer submissionId) {
}
