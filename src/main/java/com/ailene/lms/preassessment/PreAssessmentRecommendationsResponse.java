package com.ailene.lms.preassessment;

import java.time.OffsetDateTime;
import java.util.Map;

public record PreAssessmentRecommendationsResponse(PreAssessmentReportStatus status,
        Map<String, Object> recommendations, String errorMessage, OffsetDateTime generatedAt) {
}
