package com.ailene.lms.champion;

import java.time.Instant;

public interface PromptSubmissionDetailProjection {
    Integer getId();

    String getAccessId();

    String getFullName();

    String getEmail();

    String getAvatar();

    Integer getItemId();

    String getItemName();

    String getItemText();

    String getExpectedOutput();

    Integer getLevelId();

    Short getLevelNumber();

    String getLevelName();

    String getReviewerName();

    String getReviewerAvatar();

    String getReviewerAccessId();

    Instant getDeadline();

    String getMessage();

    String getInput();

    String getOutput();

    Instant getSubmittedAt();

    Instant getReviewedAt();

    String getComment();

    Boolean getAccepted();

    Short getRubricSpecificity();

    Short getRubricContext();

    Short getRubricConstraints();

    Short getRubricExamples();

    Short getRubricIteration();
}
