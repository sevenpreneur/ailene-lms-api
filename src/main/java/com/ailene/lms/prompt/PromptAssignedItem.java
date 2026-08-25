package com.ailene.lms.prompt;

import com.ailene.lms.common.AssignedByUser;
import com.ailene.lms.common.CategorySummary;

import java.time.OffsetDateTime;
import java.util.List;

public record PromptAssignedItem(Integer id, String name, String description, Integer levelId, Short levelNumber,
        List<CategorySummary> categories, Short xpReward, Boolean isAccepted, OffsetDateTime deadlineAt,
        OffsetDateTime reviewedAt, OffsetDateTime submittedAt, AssignedByUser assignedBy) {
}
