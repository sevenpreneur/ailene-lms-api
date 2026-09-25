package com.ailene.lms.prompt;

import com.ailene.lms.common.TimeUtils;

import java.time.OffsetDateTime;

public record PromptEvaluation(String aiStatus, String aiFeedback, OffsetDateTime aiEvaluatedAt, Short specificity,
        Short context, Short constraints, Short examples, Short iteration, Double average) {

    public static PromptEvaluation from(PromptEvaluationRow row) {
        return new PromptEvaluation(row.getAiStatus(), row.getAiFeedback(),
                TimeUtils.toOffsetDateTime(row.getAiEvaluatedAt()), row.getRubricSpecificity(), row.getRubricContext(),
                row.getRubricConstraints(), row.getRubricExamples(), row.getRubricIteration(),
                average(row.getRubricSpecificity(), row.getRubricContext(), row.getRubricConstraints(),
                        row.getRubricExamples(), row.getRubricIteration()));
    }

    // Only a complete set of five averages; a partial one would read as a real but misleading score.
    private static Double average(Short... scores) {
        int sum = 0;
        for (Short score : scores) {
            if (score == null) {
                return null;
            }
            sum += score;
        }
        return Math.round(sum * 10.0 / scores.length) / 10.0;
    }
}
