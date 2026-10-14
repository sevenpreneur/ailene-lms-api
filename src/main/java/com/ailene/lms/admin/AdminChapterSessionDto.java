package com.ailene.lms.admin;

import com.ailene.lms.chapter.ChapterSession;
import com.ailene.lms.common.Status;

import java.time.OffsetDateTime;

public record AdminChapterSessionDto(Integer id, Integer chapterId, Integer onlyGroupId, OffsetDateTime sessionDate,
        Status status, Boolean isLocked, OffsetDateTime updatedAt) {

    public static AdminChapterSessionDto from(ChapterSession session) {
        return new AdminChapterSessionDto(session.getId(), session.getChapterId(), session.getOnlyGroupId(),
                session.getSessionDate(), session.getStatus(), session.getIsLocked(), session.getUpdatedAt());
    }
}
