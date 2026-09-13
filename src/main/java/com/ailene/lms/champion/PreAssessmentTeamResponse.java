package com.ailene.lms.champion;

import java.time.Instant;
import java.util.List;

public record PreAssessmentTeamResponse(int departmentCount, int totalMembers, int completedCount,
        Instant measuredAt, double target, double teamAvg, int readyCount, int gapLargeCount,
        List<TeamDepartmentBaseline> departments, List<TeamPillarScore> teamPillars, TeamReadiness readiness) {
}
