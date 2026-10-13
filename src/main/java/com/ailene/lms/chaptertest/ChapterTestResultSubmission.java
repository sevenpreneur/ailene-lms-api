package com.ailene.lms.chaptertest;

import java.time.OffsetDateTime;

public record ChapterTestResultSubmission(Short score, Short correctCount, Short choiceCount,
        OffsetDateTime submittedAt) {

    public static ChapterTestResultSubmission from(ChapterTestSubmission submission) {
        return new ChapterTestResultSubmission(submission.getScore(), submission.getCorrectCount(),
                submission.getChoiceCount(), submission.getSubmittedAt());
    }
}
