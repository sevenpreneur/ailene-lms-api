package com.ailene.lms.usecase;

import java.util.List;

public record GuidedWorkflow(String summary, Integer estimatedMinutesWithAi, List<String> risks,
        List<GuidedStep> steps) {
}
