package com.ailene.lms.champion;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

public record AssignmentSpec(@NotNull AssignmentTargetType targetType, List<String> targetAccessIds,
        List<Integer> targetGroupIds, @NotNull OffsetDateTime deadline, @Size(max = 500) String message) {
}
