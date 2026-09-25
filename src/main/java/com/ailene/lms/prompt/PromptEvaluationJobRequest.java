package com.ailene.lms.prompt;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

// Explicit names: QStashClient serializes with its own RestClient, which ignores the app's SNAKE_CASE setting.
public record PromptEvaluationJobRequest(@JsonProperty("submission_id") @NotNull Integer submissionId,
        @JsonProperty("submitted_at_millis") @NotNull Long submittedAtMillis) {
}
