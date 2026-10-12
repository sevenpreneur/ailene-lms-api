package com.ailene.lms.sponsor;

import com.ailene.lms.preassessment.PreAssessmentBaseline;

import java.time.Instant;
import java.util.List;

public record PreAssessmentOrganizationResponse(int departmentCount, int totalMembers, int submittedCount,
        int scoredCount, Instant measuredAt, PreAssessmentBaseline.Band targetBand, int meetsTargetCount,
        Double orgAvg, List<PreAssessmentBaseline.Cluster> orgClusters,
        List<PreAssessmentBaseline.BandCount> bands, List<DepartmentBaseline> departments) {
}
