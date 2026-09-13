package com.ailene.lms.champion;

import com.ailene.lms.common.CategorySummary;

import java.time.Instant;
import java.util.List;

public record ReviewQueueItem(Integer id, ReviewQueueSubject subject, ReviewQueueMember member, Instant deadline,
        Instant submittedAt, Instant reviewedAt, boolean isAccepted, Double hoursWithAi, String aiTool,
        List<CategorySummary> categories) {
}
