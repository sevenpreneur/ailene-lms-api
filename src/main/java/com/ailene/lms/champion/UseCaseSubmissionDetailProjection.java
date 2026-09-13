package com.ailene.lms.champion;

import java.time.Instant;

public interface UseCaseSubmissionDetailProjection {
    Integer getId();

    String getAccessId();

    String getFullName();

    String getEmail();

    String getAvatar();

    Integer getItemId();

    String getItemName();

    String getItemText();

    Integer getLevelId();

    Short getLevelNumber();

    String getLevelName();

    String getReviewerName();

    String getReviewerAvatar();

    String getReviewerAccessId();

    Instant getDeadline();

    String getMessage();

    String getOutcomeProof();

    Double getHoursWithAi();

    Double getHoursWithoutAi();

    String getDescription();

    String getAiTool();

    String getFrequency();

    String getType();

    Instant getSubmittedAt();

    Instant getReviewedAt();

    String getComment();

    Boolean getAccepted();
}
