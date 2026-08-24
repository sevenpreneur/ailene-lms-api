package com.ailene.lms.usecase;

import com.ailene.lms.common.CategorySummary;

import java.time.OffsetDateTime;
import java.util.List;

public record UseCaseListItem(Integer id, String name, String description, Short levelNumber,
        List<CategorySummary> categories, OffsetDateTime deadlineAt, OffsetDateTime submittedAt,
        Boolean isAccepted) {
}
