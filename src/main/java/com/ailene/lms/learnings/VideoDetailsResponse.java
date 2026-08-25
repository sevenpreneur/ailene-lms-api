package com.ailene.lms.learnings;

import com.ailene.lms.common.ChapterSummary;

import java.time.OffsetDateTime;

public record VideoDetailsResponse(Integer id, String title, String description, String videoUrl, Short xpReward,
        Short orderIndex, ChapterSummary chapter, Boolean completed, OffsetDateTime completedAt,
        OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}
