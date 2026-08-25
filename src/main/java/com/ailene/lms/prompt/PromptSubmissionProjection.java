package com.ailene.lms.prompt;

import java.time.Instant;

public interface PromptSubmissionProjection {
    Integer getPromptId();

    Instant getDeadlineAt();

    Instant getSubmittedAt();

    Instant getReviewedAt();

    Boolean getIsAccepted();
}
