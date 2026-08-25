package com.ailene.lms.learnings;

import com.ailene.lms.common.ChapterSummary;

import java.util.List;

public record QuizDetailsResponse(String id, String name, String description, Short orderIndex,
        ChapterSummary chapter, Long questionCount, Long xpReward, Long attempts, List<QuizQuestionItem> questions) {
}
