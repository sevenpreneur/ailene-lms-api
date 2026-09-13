package com.ailene.lms.champion;

import java.util.List;

public record TeamDepartmentBaseline(int id, String name, int memberCount, int completedCount,
        int completionPercent, double avg, List<TeamPillarScore> pillars, List<TeamMemberBaseline> members) {
}
