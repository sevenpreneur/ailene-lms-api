package com.ailene.lms.sponsor;

import java.util.List;

public record HeadlineResponse(int productivePercent, int productiveCount, int memberCount,
        double hoursSavedLastWeek, long roiAnnualized, List<TrendPoint> trend) {
}
