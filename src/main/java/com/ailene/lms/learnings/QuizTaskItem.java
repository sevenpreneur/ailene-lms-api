package com.ailene.lms.learnings;

import com.ailene.lms.common.TimeUtils;
import com.ailene.lms.quiz.QuizTaskProjection;

import java.time.OffsetDateTime;

public record QuizTaskItem(String id, String name, String description, Short orderIndex, Long questionCount,
        Long xpReward, Integer xpEarned, Integer bestScore, Long attempts, OffsetDateTime activeAttemptStartedAt) {

    public static QuizTaskItem from(QuizTaskProjection projection) {
        return new QuizTaskItem(projection.getId(), projection.getName(), projection.getDescription(),
                projection.getOrderIndex(), projection.getQuestionCount(), projection.getXpReward(),
                projection.getXpEarned(), projection.getBestScore(), projection.getAttempts(),
                TimeUtils.toOffsetDateTime(projection.getActiveAttemptStartedAt()));
    }
}
