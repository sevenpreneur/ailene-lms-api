package com.ailene.lms.chaptertest;

import com.ailene.lms.common.ChapterSummary;

import java.util.List;

public record ChapterTestDetailsResponse(Integer id, ChapterTestKind kind, String name, String description,
        ChapterSummary chapter, Integer questionCount, Integer choiceCount, Integer essayCount, Boolean submitted,
        Boolean available, ChapterTestLockedReason lockedReason, List<ChapterTestQuestionItem> questions) {
}
