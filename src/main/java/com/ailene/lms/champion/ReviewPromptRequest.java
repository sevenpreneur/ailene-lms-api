package com.ailene.lms.champion;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReviewPromptRequest(@NotBlank String projectId, @NotNull @Positive Integer submissionId,
        @NotNull Boolean isAccepted, @Size(max = 2000) String comment,
        @Min(1) @Max(5) Short rubricSpecificity, @Min(1) @Max(5) Short rubricContext,
        @Min(1) @Max(5) Short rubricConstraints, @Min(1) @Max(5) Short rubricExamples,
        @Min(1) @Max(5) Short rubricIteration) {
}
