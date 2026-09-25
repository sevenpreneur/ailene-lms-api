package com.ailene.lms.champion;

import com.ailene.lms.common.CategorySummary;
import com.ailene.lms.prompt.PromptEvaluation;

import java.time.Instant;
import java.util.List;

public record ReviewQueueItem(Integer id, ReviewQueueSubject subject, ReviewQueueMember member, Instant deadline,
        Instant submittedAt, Instant reviewedAt, boolean isAccepted, Double hoursWithAi, String aiTool,
        PromptEvaluation evaluation, List<CategorySummary> categories) {
}
