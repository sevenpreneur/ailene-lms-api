package com.ailene.lms.quiz;

import com.ailene.lms.common.ChapterSummary;

import java.util.List;

public record QuizResultResponse(String id, String name, String description, ChapterSummary chapter,
        List<QuizResultQuestionItem> questions, QuizResultSubmission submission, Short xpEarned) {
}
