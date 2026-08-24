package com.ailene.lms.quiz;

public interface QuizTaskProjection {
    String getId();

    String getName();

    String getDescription();

    Short getOrderIndex();

    Long getQuestionCount();

    Long getXpReward();

    Integer getXpEarned();

    Integer getBestScore();

    Long getAttempts();
}
