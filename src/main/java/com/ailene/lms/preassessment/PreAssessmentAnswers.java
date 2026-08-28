package com.ailene.lms.preassessment;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

public record PreAssessmentAnswers(Integer id, String accessId, PreAssessmentAiUseFrequency aiUseFrequency,
        List<String> aiToolsUsed, List<String> aiLimitations, PreAssessmentOutputReview outputReview,
        List<String> useCases, PreAssessmentTeamAdoption teamAdoption, String concreteExample,
        PreAssessmentFrequency modelSelection, PreAssessmentFrequency multimodalUse,
        PreAssessmentFrequency workflowReuse, PreAssessmentPromptSkill promptComfort,
        PreAssessmentFrequency promptIteration, PreAssessmentRefineScenario refineScenario,
        PreAssessmentAttitude professionalAttitude, PreAssessmentFrequency dataSafetyCheck,
        PreAssessmentFrequency publishUnchecked, String biggestChallenge, String trainingExpectation,
        PreAssessmentMotivation motivation, OffsetDateTime createdAt) {

    public static PreAssessmentAnswers from(PreAssessment pa) {
        return new PreAssessmentAnswers(pa.getId(), pa.getAccessId(), pa.getAiUseFrequency(),
                toList(pa.getAiToolsUsed()), toList(pa.getAiLimitations()), pa.getOutputReview(),
                toList(pa.getUseCases()), pa.getTeamAdoption(), pa.getConcreteExample(), pa.getModelSelection(),
                pa.getMultimodalUse(), pa.getWorkflowReuse(), pa.getPromptComfort(), pa.getPromptIteration(),
                pa.getRefineScenario(), pa.getProfessionalAttitude(), pa.getDataSafetyCheck(),
                pa.getPublishUnchecked(), pa.getBiggestChallenge(), pa.getTrainingExpectation(), pa.getMotivation(),
                pa.getCreatedAt());
    }

    private static List<String> toList(String[] values) {
        return values == null ? List.of() : Arrays.asList(values);
    }
}
