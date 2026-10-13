package com.ailene.lms.chaptertest;

import java.util.List;

public record ChapterTestResultQuestionItem(Integer id, ChapterTestQuestionType type, String question,
        Short orderIndex, String explanation, List<ChapterTestResultOptionItem> options, String answer,
        Boolean isCorrect) {

    // Without reveal, correctness and explanations are withheld so the post-test can still measure something.
    public static ChapterTestResultQuestionItem from(ChapterTestQuestion question, String answer, boolean reveal) {
        List<ChapterTestResultOptionItem> options = question.options().stream()
                .map(o -> new ChapterTestResultOptionItem(o.id(), o.optionCode(), o.text(),
                        reveal ? o.correct() : null))
                .toList();
        Boolean isCorrect = null;
        if (reveal && question.isMultipleChoice()) {
            isCorrect = question.options().stream()
                    .anyMatch(o -> o.correct() && o.optionCode().equals(answer));
        }
        return new ChapterTestResultQuestionItem(question.id(), question.type(), question.question(),
                question.orderIndex(), reveal ? question.explanation() : null, options, answer, isCorrect);
    }
}
