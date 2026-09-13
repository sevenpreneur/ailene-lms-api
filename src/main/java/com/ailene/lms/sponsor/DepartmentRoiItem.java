package com.ailene.lms.sponsor;

public record DepartmentRoiItem(int id, String name, int memberCount, double hoursSavedWeekly,
        double hoursSavedTotal, long roiAnnualized, int contributionPercent) {
}
