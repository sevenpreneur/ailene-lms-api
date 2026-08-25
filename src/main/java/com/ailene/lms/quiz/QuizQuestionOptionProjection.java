package com.ailene.lms.quiz;

public interface QuizQuestionOptionProjection {
    Integer getQuestionId();

    String getQuestion();

    Short getQuestionOrderIndex();

    Short getQuestionXpReward();

    Integer getOptionId();

    String getOptionCode();

    String getOptionText();
}
