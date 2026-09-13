package com.ailene.lms.champion;

import java.time.Instant;

public interface MemberSubmissionProjection {
    Integer getId();

    String getItemName();

    String getAiTool();

    Instant getSubmittedAt();

    Instant getReviewedAt();

    Boolean getAccepted();

    Instant getCreatedAt();
}
