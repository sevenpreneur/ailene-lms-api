package com.ailene.lms.material;

import com.ailene.lms.common.ChapterSummary;

import java.time.OffsetDateTime;

public record MaterialDetailsResponse(String id, String title, String description, String content, String fileUrl,
        String imageUrl, Short xpReward, Short orderIndex, ChapterSummary chapter, Boolean completed,
        OffsetDateTime completedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}
