package com.ailene.lms.champion;

public record CreateUseCaseAssignmentResponse(Integer useCaseId, int assignedCount, int targetTotal, int skipped) {
}
