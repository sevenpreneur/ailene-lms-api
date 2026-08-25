package com.ailene.lms.prompt;

import com.ailene.lms.common.CategorySummary;

import java.time.OffsetDateTime;
import java.util.List;

public record PromptListItem(Integer id, String name, String description, Short levelNumber,
        List<CategorySummary> categories, OffsetDateTime deadlineAt, OffsetDateTime submittedAt,
        OffsetDateTime reviewedAt, Boolean isAccepted) {
}
