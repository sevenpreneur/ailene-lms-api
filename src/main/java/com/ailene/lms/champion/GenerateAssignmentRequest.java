package com.ailene.lms.champion;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenerateAssignmentRequest(@NotBlank String projectId, @NotBlank @Size(max = 2000) String instruction,
        AssignmentKind kind, @Min(1) @Max(5) Integer count) {
}
