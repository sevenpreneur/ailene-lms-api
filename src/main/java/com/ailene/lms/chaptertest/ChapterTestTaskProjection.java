package com.ailene.lms.chaptertest;

import java.time.Instant;

public interface ChapterTestTaskProjection {
    Integer getId();

    String getKind();

    String getName();

    String getDescription();

    Long getQuestionCount();

    Short getScore();

    Instant getSubmittedAt();
}
