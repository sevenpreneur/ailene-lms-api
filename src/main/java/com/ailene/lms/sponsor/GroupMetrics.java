package com.ailene.lms.sponsor;

public record GroupMetrics(int totalMembers, int activeMembers, int activePercent, double avgLevel,
        int beginnerCount, int beginnerPercent, double hoursSavedTotal, int acceptedUseCases,
        int acceptedUseCasesThisMonth, boolean needsIntervention) {
}
