package com.ailene.lms.prompt;

import java.time.Instant;

public interface PromptEvaluationRow {
    Short getRubricSpecificity();

    Short getRubricContext();

    Short getRubricConstraints();

    Short getRubricExamples();

    Short getRubricIteration();

    String getAiStatus();

    String getAiFeedback();

    Instant getAiEvaluatedAt();
}
