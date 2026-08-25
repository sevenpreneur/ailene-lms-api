package com.ailene.lms.prompt;

import java.time.Instant;
import java.util.UUID;

public interface PromptAssignedProjection {
    Integer getId();

    String getName();

    String getDescription();

    Integer getLevelId();

    Short getLevelNumber();

    Short getXpReward();

    Instant getDeadlineAt();

    Instant getSubmittedAt();

    Instant getReviewedAt();

    Boolean getIsAccepted();

    UUID getAssignedById();

    String getAssignedByName();

    String getAssignedByAvatar();
}
