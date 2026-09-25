package com.ailene.lms.prompt;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PromptEvaluationTest {

    @Test
    void from_completeRubric_averagesToOneDecimal() {
        PromptEvaluation evaluation = PromptEvaluation.from(row(new Short[] { 4, 5, 2, 1, 3 }));

        assertThat(evaluation.context()).isEqualTo((short) 5);
        assertThat(evaluation.average()).isEqualTo(3.0);
        assertThat(evaluation.aiStatus()).isEqualTo("completed");
    }

    @Test
    void from_pendingEvaluation_hasNoAverage() {
        PromptEvaluation evaluation = PromptEvaluation.from(row(new Short[5]));

        assertThat(evaluation.specificity()).isNull();
        assertThat(evaluation.average()).isNull();
    }

    private static PromptEvaluationRow row(Short[] rubric) {
        return new PromptEvaluationRow() {
            public Short getRubricSpecificity() { return rubric[0]; }
            public Short getRubricContext() { return rubric[1]; }
            public Short getRubricConstraints() { return rubric[2]; }
            public Short getRubricExamples() { return rubric[3]; }
            public Short getRubricIteration() { return rubric[4]; }
            public String getAiStatus() { return rubric[0] == null ? "pending" : "completed"; }
            public String getAiFeedback() { return null; }
            public Instant getAiEvaluatedAt() { return null; }
        };
    }
}
