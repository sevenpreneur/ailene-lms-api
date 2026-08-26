package com.ailene.lms.quiz;

public interface QuizAnswerKeyProjection {
    Integer getQuestionId();

    Short getXpReward();

    String getCorrectOptionCode();
}
