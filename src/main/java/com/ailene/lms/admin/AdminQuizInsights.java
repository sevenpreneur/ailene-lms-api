package com.ailene.lms.admin;

import java.time.OffsetDateTime;
import java.util.List;

public record AdminQuizInsights(Summary summary, List<Bucket> distribution, List<QuizRow> quizzes,
        List<Submission> submissions) {

    public record Summary(int submissions, int participants, Double averageScore, Double averageBestScore,
            int perfectScores) {
    }

    public record Bucket(String label, int min, int max, int count) {
    }

    public record QuizRow(String id, String name, AdminInsightsChapter chapter, int questionCount, int attempts,
            int participants, Double averageScore, Double averageBestScore, Integer highestScore) {
    }

    public record Submission(Integer id, AdminInsightsLearner learner, String quizId, String quizName,
            AdminInsightsChapter chapter, Integer attemptNumber, Integer score, OffsetDateTime startedAt,
            OffsetDateTime submittedAt) {
    }
}
