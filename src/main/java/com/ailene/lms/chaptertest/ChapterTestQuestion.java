package com.ailene.lms.chaptertest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ChapterTestQuestion(Integer id, ChapterTestQuestionType type, String question, String explanation,
        Short orderIndex, List<Option> options) {

    public record Option(Integer id, String optionCode, String text, boolean correct) {
    }

    // Folds the one-row-per-option join back into questions, keeping the query's order.
    public static List<ChapterTestQuestion> group(List<ChapterTestQuestionOptionProjection> rows) {
        Map<Integer, ChapterTestQuestionOptionProjection> firstRowByQuestion = new LinkedHashMap<>();
        Map<Integer, List<Option>> optionsByQuestion = new LinkedHashMap<>();
        for (ChapterTestQuestionOptionProjection row : rows) {
            firstRowByQuestion.putIfAbsent(row.getQuestionId(), row);
            List<Option> options = optionsByQuestion.computeIfAbsent(row.getQuestionId(), id -> new ArrayList<>());
            if (row.getOptionId() != null) {
                options.add(new Option(row.getOptionId(), row.getOptionCode(), row.getOptionText(),
                        Boolean.TRUE.equals(row.getOptionIsCorrect())));
            }
        }
        return firstRowByQuestion.values().stream()
                .map(row -> new ChapterTestQuestion(row.getQuestionId(),
                        ChapterTestQuestionType.valueOf(row.getType()), row.getQuestion(), row.getExplanation(),
                        row.getOrderIndex(), List.copyOf(optionsByQuestion.get(row.getQuestionId()))))
                .toList();
    }

    public boolean isMultipleChoice() {
        return type == ChapterTestQuestionType.multiple_choice;
    }
}
