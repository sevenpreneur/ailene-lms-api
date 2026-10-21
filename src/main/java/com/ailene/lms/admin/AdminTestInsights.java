package com.ailene.lms.admin;

import java.time.OffsetDateTime;
import java.util.List;

public record AdminTestInsights(Summary summary, List<ChapterRow> chapters, List<Row> results) {

    public record Summary(int preSubmissions, int postSubmissions, Double preAverage, Double postAverage,
            Double averageUplift, int improvedCount, int pairedCount) {
    }

    public record ChapterRow(AdminInsightsChapter chapter, boolean hasPreTest, boolean hasPostTest,
            int preSubmissions, int postSubmissions, Double preAverage, Double postAverage, Double averageUplift) {
    }

    public record Row(AdminInsightsLearner learner, AdminInsightsChapter chapter, boolean preSubmitted,
            Integer preScore, OffsetDateTime preSubmittedAt, boolean postSubmitted, Integer postScore,
            OffsetDateTime postSubmittedAt, Integer uplift) {
    }
}
