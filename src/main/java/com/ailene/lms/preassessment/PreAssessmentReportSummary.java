package com.ailene.lms.preassessment;

import java.util.List;

public record PreAssessmentReportSummary(List<PreAssessmentReportPillar> pillars, double avg,
        PreAssessmentPillarRef strongest, PreAssessmentPillarRef weakest, String quote) {
}
