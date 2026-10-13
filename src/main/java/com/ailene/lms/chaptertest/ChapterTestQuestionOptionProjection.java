package com.ailene.lms.chaptertest;

public interface ChapterTestQuestionOptionProjection {
    Integer getQuestionId();

    String getType();

    String getQuestion();

    String getExplanation();

    Short getOrderIndex();

    Integer getOptionId();

    String getOptionCode();

    String getOptionText();

    Boolean getOptionIsCorrect();
}
