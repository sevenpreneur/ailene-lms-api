package com.ailene.lms.sponsor;

import java.util.List;

public record DepartmentPillars(int id, String name, int memberCount, int completedCount, int completionPercent,
        List<PillarScore> pillars, double avg) {
}
