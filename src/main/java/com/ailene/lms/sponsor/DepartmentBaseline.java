package com.ailene.lms.sponsor;

import com.ailene.lms.preassessment.PreAssessmentBaseline;

import java.util.List;

public record DepartmentBaseline(Integer id, String name, int memberCount, int submittedCount, int scoredCount,
        int completionPercent, int meetsTargetCount, Double avg, List<PreAssessmentBaseline.Cluster> clusters,
        List<PreAssessmentBaseline.BandCount> bands) {
}
