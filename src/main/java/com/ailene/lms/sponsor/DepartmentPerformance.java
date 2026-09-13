package com.ailene.lms.sponsor;

public record DepartmentPerformance(int rank, int id, String name, int memberCount, double avgScore,
        String topUseCase, int submissionCount, double hours, Integer trendPercent) {
}
