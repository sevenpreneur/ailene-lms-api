package com.ailene.lms.survey;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ChapterSurveySubmitRequest(
        @NotNull @Positive Integer chapterId,
        @NotNull @Min(1) @Max(5) Short trainerClarity,
        @NotNull @Min(1) @Max(5) Short trainerMastery,
        @NotNull @Min(1) @Max(5) Short trainerResponsiveness,
        @NotNull @Min(1) @Max(5) Short trainerEngagement,
        @NotNull @Min(1) @Max(5) Short materialRelevance,
        @NotNull @Min(1) @Max(5) Short materialFlow,
        @NotNull @Min(1) @Max(5) Short materialDepth,
        @NotNull @Min(1) @Max(5) Short learningValue,
        @Size(max = 2000) String missingTopics,
        @Size(max = 2000) String favoriteMaterial,
        @Size(max = 2000) String dislikedMaterial,
        @Size(max = 2000) String improvementSuggestion) {
}
