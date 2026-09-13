package com.ailene.lms.champion;

public record CreatePromptAssignmentResponse(Integer promptId, int assignedCount, int targetTotal, int skipped) {
}
