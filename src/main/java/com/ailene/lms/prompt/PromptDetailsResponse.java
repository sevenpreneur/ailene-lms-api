package com.ailene.lms.prompt;

import com.ailene.lms.common.CategorySummary;

import java.time.OffsetDateTime;
import java.util.List;

public record PromptDetailsResponse(Integer id, String name, String scenario, String expectedOutput,
        Integer levelId, Short levelNumber, List<CategorySummary> categories, Short xpReward, Boolean isSelfCreated,
        OffsetDateTime deadlineAt, OffsetDateTime submittedAt, OffsetDateTime reviewedAt, Boolean isAccepted,
        String input, String output, String comment, PromptEvaluation evaluation) {
}
