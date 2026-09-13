package com.ailene.lms.champion;

public record ReportMetrics(int activeMembers, int totalMembers, int activePercent, int acceptedSubmissions,
        double hoursSaved, int levelUps) {
}
