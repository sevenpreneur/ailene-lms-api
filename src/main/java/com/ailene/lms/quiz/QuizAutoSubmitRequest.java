package com.ailene.lms.quiz;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record QuizAutoSubmitRequest(@JsonProperty("submission_id") @NotNull Integer submissionId) {
}
