package com.ailene.lms.champion;

import jakarta.validation.constraints.NotBlank;

public record ChampionReportRequest(@NotBlank String projectId, ReportPeriod period) {

    public ReportPeriod periodOrDefault() {
        return period == null ? ReportPeriod.weekly : period;
    }
}
