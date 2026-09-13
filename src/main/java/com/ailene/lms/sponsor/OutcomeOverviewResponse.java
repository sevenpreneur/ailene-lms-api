package com.ailene.lms.sponsor;

public record OutcomeOverviewResponse(int memberCount, int departmentCount, double hoursSavedTotal,
        double fteEquivalent, long roiTotal, long roiRatePerHour, double avgLevel, short maxLevelNumber,
        int certifiedCount, int certifiedPercent) {
}
