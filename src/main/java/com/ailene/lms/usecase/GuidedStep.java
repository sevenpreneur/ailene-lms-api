package com.ailene.lms.usecase;

import java.time.OffsetDateTime;

public record GuidedStep(Integer id, int order, String title, String instruction, String suggestedTool,
        String promptTemplate, int estimatedMinutes, String verification, boolean isDone,
        String skippedReason, String note, OffsetDateTime doneAt) {
}
