package com.ailene.lms.preassessment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record PreAssessmentScoreResponse(Integer attemptId, PreAssessmentAttemptStatus status,
        OffsetDateTime submittedAt, OffsetDateTime scoredAt, Band band, Band rawBand, Band targetBand, Integer gap,
        Boolean meetsTarget, Dimensions dimensions, LevelMastery levelMastery, Gates gates, Flags flags,
        List<Cluster> clusters, List<Domain> domains, List<Feedback> feedback) {

    public record Band(Integer band, String name, Integer level) {
    }

    public record Dimensions(BigDecimal knowledge, BigDecimal skills, BigDecimal behaviours) {
    }

    public record LevelMastery(BigDecimal p1, BigDecimal p2, BigDecimal p3) {
    }

    public record Gates(Boolean securityPassed, Boolean behaviourPassed) {
    }

    public record Flags(Boolean overclaim, Boolean shadowAi, Boolean sensitiveData) {
    }

    public record Cluster(String code, String name, BigDecimal score) {
    }

    public record Domain(String code, String name, String clusterCode, BigDecimal score) {
    }

    public record Feedback(String itemCode, String text) {
    }
}
