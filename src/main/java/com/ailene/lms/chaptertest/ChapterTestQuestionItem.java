package com.ailene.lms.chaptertest;

import java.util.List;

public record ChapterTestQuestionItem(Integer id, ChapterTestQuestionType type, String question, Short orderIndex,
        List<ChapterTestOptionItem> options) {

    public static ChapterTestQuestionItem from(ChapterTestQuestion question) {
        return new ChapterTestQuestionItem(question.id(), question.type(), question.question(),
                question.orderIndex(), question.options().stream().map(ChapterTestOptionItem::from).toList());
    }
}
