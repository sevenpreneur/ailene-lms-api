package com.ailene.lms.sponsor;

import java.time.Instant;

public interface PromptRowProjection {
    String getAccessId();

    Integer getGroupId();

    Instant getSubmittedAt();

    Instant getReviewedAt();

    Boolean getAccepted();
}
