package com.ailene.lms.usecase;

import java.time.Instant;
import java.util.UUID;

public interface UseCaseAssignedProjection {
    Integer getId();

    String getName();

    String getDescription();

    Short getXpReward();

    Instant getDeadlineAt();

    Instant getSubmittedAt();

    Instant getReviewedAt();

    Boolean getIsAccepted();

    UUID getAssignedById();

    String getAssignedByName();

    String getAssignedByAvatar();
}
