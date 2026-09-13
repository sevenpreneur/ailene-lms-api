package com.ailene.lms.sponsor;

import java.time.Instant;
import java.util.List;

public record PreAssessmentOrganizationResponse(int departmentCount, int totalMembers, int completedCount,
        Instant measuredAt, double target, double orgAvg, int readyCount, int gapLargeCount,
        List<DepartmentPillars> departments, List<PillarScore> orgPillars, Readiness readiness) {
}
