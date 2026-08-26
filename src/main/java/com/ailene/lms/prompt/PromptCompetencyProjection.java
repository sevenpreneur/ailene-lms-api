package com.ailene.lms.prompt;

import java.time.Instant;

public interface PromptCompetencyProjection {
    Instant getReviewedAt();

    Short getRubricSpecificity();

    Short getRubricContext();

    Short getRubricConstraints();

    Short getRubricExamples();

    Short getRubricIteration();
}
