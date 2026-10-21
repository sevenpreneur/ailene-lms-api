package com.ailene.lms.admin;

import java.time.OffsetDateTime;
import java.util.List;

public record AdminReviewInsights(Summary summary, List<ChapterRow> chapters, List<Response> responses) {

    public record Summary(int learnerCount, int responses, Double trainerAverage, Double materialAverage,
            Double learningValueAverage, Double overallAverage) {
    }

    public record Ratings(Double trainerClarity, Double trainerMastery, Double trainerResponsiveness,
            Double trainerEngagement, Double materialRelevance, Double materialFlow, Double materialDepth,
            Double learningValue) {
    }

    public record ChapterRow(AdminInsightsChapter chapter, int responses, Ratings ratings, Double trainerAverage,
            Double materialAverage, Double overallAverage) {
    }

    public record Response(Integer id, AdminInsightsLearner learner, AdminInsightsChapter chapter, Ratings ratings,
            Double overallAverage, String missingTopics, String favoriteMaterial, String dislikedMaterial,
            String improvementSuggestion, OffsetDateTime submittedAt) {
    }
}
