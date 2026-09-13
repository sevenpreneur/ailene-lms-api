package com.ailene.lms.champion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

public record AssignLibraryRequest(@NotBlank String projectId, @NotNull @Positive Integer libraryId,
        @NotNull AssignmentTargetType targetType, List<String> targetAccessIds, List<Integer> targetGroupIds,
        @NotNull OffsetDateTime deadline, @Size(max = 500) String message) {
}
