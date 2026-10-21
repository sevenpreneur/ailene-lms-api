package com.ailene.lms.admin;

public record AdminInsightsOverview(int learnerCount, AdminPreAssessmentInsights.Summary preAssessment,
        AdminGuidedInsights.Summary guidedUseCases, AdminReviewInsights.Summary sessionReviews,
        AdminTestInsights.Summary chapterTests, AdminQuizInsights.Summary quizzes) {
}
