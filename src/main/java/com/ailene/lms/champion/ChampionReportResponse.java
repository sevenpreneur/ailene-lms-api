package com.ailene.lms.champion;

import java.time.Instant;
import java.util.List;

public record ChampionReportResponse(ReportPeriod period, Instant generatedAt, ReportInfo report,
        ReportRecipient recipient, ReportMetrics metrics, List<LevelMovement> levelMovements, String narrative,
        List<SentReport> sentReports, Instant previousPeriodStart) {
}
