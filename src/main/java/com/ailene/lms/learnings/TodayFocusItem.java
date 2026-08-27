package com.ailene.lms.learnings;

import com.ailene.lms.common.AssignedByUser;

import java.time.OffsetDateTime;

public record TodayFocusItem(TodayFocusKind kind, String taskId, String taskTitle, Integer chapterId,
        String chapterName, Integer levelId, Short levelNumber, String category, AssignedByUser assignedBy,
        OffsetDateTime deadline) {
}
