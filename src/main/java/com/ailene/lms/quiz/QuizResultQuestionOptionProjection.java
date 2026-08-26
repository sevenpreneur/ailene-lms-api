package com.ailene.lms.quiz;

public interface QuizResultQuestionOptionProjection {
    Integer getQuestionId();

    String getQuestion();

    Short getQuestionOrderIndex();

    Short getQuestionXpReward();

    String getExplanation();

    Integer getOptionId();

    String getOptionCode();

    String getOptionText();

    Boolean getOptionIsCorrect();
}
