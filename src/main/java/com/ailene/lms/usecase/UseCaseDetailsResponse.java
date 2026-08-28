package com.ailene.lms.usecase;

import com.ailene.lms.common.CategorySummary;

import java.time.OffsetDateTime;
import java.util.List;

public record UseCaseDetailsResponse(Integer id, String name, String description, Integer levelId,
        Short levelNumber, List<CategorySummary> categories, Short xpReward, Boolean isSelfCreated,
        OffsetDateTime deadlineAt, OffsetDateTime submittedAt, OffsetDateTime reviewedAt, Boolean isAccepted) {
}
