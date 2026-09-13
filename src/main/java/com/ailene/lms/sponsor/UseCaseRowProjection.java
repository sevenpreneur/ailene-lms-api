package com.ailene.lms.sponsor;

import java.time.Instant;

public interface UseCaseRowProjection {
    String getAccessId();

    Integer getGroupId();

    Integer getUseCaseId();

    String getUseCaseName();

    Short getLevelNumber();

    String getLevelName();

    Instant getSubmittedAt();

    Instant getReviewedAt();

    Boolean getAccepted();

    Double getHoursWithAi();

    Double getHoursWithoutAi();
}
