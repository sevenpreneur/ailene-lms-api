package com.ailene.lms.prompt;

import java.time.Instant;

public interface PromptSubmissionProjection extends PromptEvaluationRow {
    Integer getPromptId();

    Instant getDeadlineAt();

    Instant getSubmittedAt();

    Instant getReviewedAt();

    Boolean getIsAccepted();

    String getInput();

    String getOutput();

    String getComment();
}
