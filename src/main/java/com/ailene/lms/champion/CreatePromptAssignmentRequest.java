package com.ailene.lms.champion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePromptAssignmentRequest(@NotBlank String projectId, @NotBlank @Size(max = 255) String name,
        @NotBlank String description, @NotBlank String expectedOutput,
        @NotEmpty @Size(max = 2) List<Short> categoryIds, @Valid AssignmentSpec assignment) {
}
