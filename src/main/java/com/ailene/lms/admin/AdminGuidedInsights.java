package com.ailene.lms.admin;

import java.time.OffsetDateTime;
import java.util.List;

public record AdminGuidedInsights(Summary summary, List<StatusCount> funnel, List<Row> sessions) {

    public enum ReviewStatus { in_progress, awaiting_review, needs_revision, accepted }

    public record Summary(int total, int inProgress, int awaitingReview, int needsRevision, int accepted,
            Double minutesSavedPerRun, Double hoursSaved, Double averageHelpfulness) {
    }

    public record StatusCount(String status, int count) {
    }

    public record Row(Integer id, AdminInsightsLearner learner, String status, Integer currentStep,
            ReviewStatus reviewStatus, String useCaseName, String dailyTask, String frequency, String painPoint,
            String summary, Double minutesWithoutAi, Integer estimatedMinutesWithAi, int stepCount, int stepsDone,
            Double hoursSaved, Integer helpfulnessRating, String testimonial, String reviewComment,
            OffsetDateTime submittedAt, OffsetDateTime reviewedAt, OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }
}
