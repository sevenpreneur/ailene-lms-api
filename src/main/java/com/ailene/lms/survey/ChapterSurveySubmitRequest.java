package com.ailene.lms.survey;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ChapterSurveySubmitRequest(
        @NotNull @Positive Integer chapterId,
        @NotNull @Min(1) @Max(5) Short satisfaction,
        @NotNull @Min(1) @Max(5) Short relevance,
        @NotNull @Min(1) @Max(5) Short trainerRating,
        @Size(max = 2000) String comment) {
}
