package com.ailene.lms.chaptertest;

import com.ailene.lms.common.ChapterSummary;

import java.util.List;

public record ChapterTestResultResponse(Integer id, ChapterTestKind kind, String name, String description,
        ChapterSummary chapter, Boolean answersRevealed, ChapterTestResultSubmission submission,
        List<ChapterTestResultQuestionItem> questions, Short preTestScore) {
}
