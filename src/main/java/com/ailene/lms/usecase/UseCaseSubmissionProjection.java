package com.ailene.lms.usecase;

import java.time.Instant;

public interface UseCaseSubmissionProjection {
    Integer getUseCaseId();

    Instant getDeadlineAt();

    Instant getSubmittedAt();

    Instant getReviewedAt();

    Boolean getIsAccepted();
}
