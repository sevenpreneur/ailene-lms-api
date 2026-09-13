package com.ailene.lms.sponsor;

import java.util.List;

public record GroupLevelRow(int id, String name, int total, int activeWeekly, int entryLevelCount,
        int entryLevelPercent, List<GroupLevelSlice> levels) {
}
