package com.ailene.lms.chaptertest;

import java.time.OffsetDateTime;

public record ChapterTestSubmitResponse(Short score, Short correctCount, Short choiceCount, Short essayCount,
        OffsetDateTime submittedAt) {
}
