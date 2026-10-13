package com.ailene.lms.chaptertest;

import com.ailene.lms.common.TimeUtils;

import java.time.OffsetDateTime;

public record ChapterTestTaskItem(Integer id, ChapterTestKind kind, String name, String description,
        Long questionCount, Boolean submitted, Short score, OffsetDateTime submittedAt, Boolean available,
        ChapterTestLockedReason lockedReason) {

    public static ChapterTestTaskItem from(ChapterTestTaskProjection row, ChapterTestLockedReason lockedReason) {
        return new ChapterTestTaskItem(row.getId(), ChapterTestKind.valueOf(row.getKind()), row.getName(),
                row.getDescription(), row.getQuestionCount(), ChapterTestPair.submitted(row), row.getScore(),
                TimeUtils.toOffsetDateTime(row.getSubmittedAt()), lockedReason == null, lockedReason);
    }
}
