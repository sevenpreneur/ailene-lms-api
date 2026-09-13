package com.ailene.lms.champion;

import java.time.Instant;

public interface TeamUseCaseProjection {
    String getAccessId();

    Instant getSubmittedAt();

    Boolean getAccepted();

    Double getHoursWithAi();

    Double getHoursWithoutAi();

    String getAiTool();
}
