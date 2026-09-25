package com.ailene.lms.champion;

import com.ailene.lms.prompt.PromptEvaluationRow;

import java.time.Instant;

public interface ReviewQueueProjection extends PromptEvaluationRow {
    Integer getId();

    String getAccessId();

    String getFullName();

    String getAvatar();

    Integer getItemId();

    String getItemName();

    String getItemText();

    Integer getLevelId();

    Short getLevelNumber();

    String getLevelName();

    Instant getDeadline();

    Instant getSubmittedAt();

    Instant getReviewedAt();

    Boolean getAccepted();

    Double getHoursWithAi();

    String getAiTool();
}
