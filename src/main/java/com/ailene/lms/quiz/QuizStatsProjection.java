package com.ailene.lms.quiz;

public interface QuizStatsProjection {
    Long getQuestionCount();

    Long getXpReward();

    Long getAttempts();
}
