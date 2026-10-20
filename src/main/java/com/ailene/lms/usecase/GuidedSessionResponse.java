package com.ailene.lms.usecase;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record GuidedSessionResponse(Integer id, Integer useCaseId, String projectId, GuidedStatus status,
        int currentStep, String dailyTask, UseCaseFrequency frequency, String exampleText, String exampleFileUrl,
        boolean sensitiveDataConfirmed, BigDecimal minutesWithoutAi, String currentProcess,
        List<String> currentTools, String painPoint, List<GuidedStep> steps, String summary,
        Integer estimatedMinutesWithAi, List<String> risks, int regenerateRemaining, boolean isGenerating,
        OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public static GuidedSessionResponse from(GuidedSession s, GuidedJson json) {
        return new GuidedSessionResponse(s.getId(), s.getUseCaseId(), s.getProjectId(), s.getStatus(),
                s.getCurrentStep(), s.getDailyTask(), s.getFrequency(), s.getExampleText(), s.getExampleFileUrl(),
                s.isSensitiveDataConfirmed(), s.getMinutesWithoutAi(), s.getCurrentProcess(),
                json.strings(s.getCurrentToolsJson()), s.getPainPoint(), json.steps(s.getStepsJson()), s.getSummary(),
                s.getEstimatedMinutesWithAi(), json.strings(s.getRisksJson()),
                Math.max(0, GuidedUseCaseStore.MAX_GENERATIONS - s.getGenerationsUsed()),
                GuidedRules.generating(s), s.getCreatedAt(), s.getUpdatedAt());
    }
}
