package com.ailene.lms.quiz;

import java.time.Instant;

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

    Instant getActiveAttemptStartedAt();
}
