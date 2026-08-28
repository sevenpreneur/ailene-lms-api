package com.ailene.lms.preassessment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PreAssessmentCreateRequest(@NotBlank String projectId,
        @NotNull PreAssessmentAiUseFrequency aiUseFrequency, @NotEmpty List<String> aiToolsUsed,
        @NotEmpty List<String> aiLimitations, @NotNull PreAssessmentOutputReview outputReview,
        @NotEmpty List<String> useCases, @NotNull PreAssessmentTeamAdoption teamAdoption,
        @Size(max = 255) String concreteExample, @NotNull PreAssessmentFrequency modelSelection,
        @NotNull PreAssessmentFrequency multimodalUse, @NotNull PreAssessmentFrequency workflowReuse,
        @NotNull PreAssessmentPromptSkill promptComfort, @NotNull PreAssessmentFrequency promptIteration,
        @NotNull PreAssessmentRefineScenario refineScenario, @NotNull PreAssessmentAttitude professionalAttitude,
        @NotNull PreAssessmentFrequency dataSafetyCheck, @NotNull PreAssessmentFrequency publishUnchecked,
        @NotBlank String biggestChallenge, @NotBlank String trainingExpectation,
        @NotNull PreAssessmentMotivation motivation) {
}
