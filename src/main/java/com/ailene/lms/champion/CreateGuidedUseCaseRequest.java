package com.ailene.lms.champion;

import com.ailene.lms.usecase.GuidedPrefill;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateGuidedUseCaseRequest(@NotBlank String projectId, @NotBlank @Size(max = 255) String name,
        @Size(max = 5000) String description, @NotEmpty @Size(max = 2) List<Short> categoryIds,
        @NotNull @Valid AssignmentSpec assignment, @NotNull @Valid GuidedPrefill intake) {
}
