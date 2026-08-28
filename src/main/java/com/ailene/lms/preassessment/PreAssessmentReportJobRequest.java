package com.ailene.lms.preassessment;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record PreAssessmentReportJobRequest(@JsonProperty("pre_assessment_id") @NotNull Integer preAssessmentId) {
}
