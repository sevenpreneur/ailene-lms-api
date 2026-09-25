package com.ailene.lms.champion;

import com.ailene.lms.prompt.PromptEvaluationRow;

import java.time.Instant;

public interface PromptSubmissionDetailProjection extends PromptEvaluationRow {
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
}
