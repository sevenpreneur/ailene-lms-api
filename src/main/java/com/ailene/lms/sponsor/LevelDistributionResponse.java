package com.ailene.lms.sponsor;

import java.util.List;

public record LevelDistributionResponse(int total, int activeWeekly, int participationPercent,
        List<LevelSlice> levels, List<GroupLevelRow> groups, List<GroupLevelRow> groupsNeedingIntervention) {
}
