package com.ailene.lms.survey;

import java.time.OffsetDateTime;

public record ChapterSurveyAnswer(Short trainerClarity, Short trainerMastery, Short trainerResponsiveness,
        Short trainerEngagement, Short materialRelevance, Short materialFlow, Short materialDepth,
        Short learningValue, String missingTopics, String favoriteMaterial, String dislikedMaterial,
        String improvementSuggestion, OffsetDateTime submittedAt) {

    public static ChapterSurveyAnswer from(ChapterSurvey survey) {
        return new ChapterSurveyAnswer(survey.getTrainerClarity(), survey.getTrainerMastery(),
                survey.getTrainerResponsiveness(), survey.getTrainerEngagement(), survey.getMaterialRelevance(),
                survey.getMaterialFlow(), survey.getMaterialDepth(), survey.getLearningValue(),
                survey.getMissingTopics(), survey.getFavoriteMaterial(), survey.getDislikedMaterial(),
                survey.getImprovementSuggestion(), survey.getSubmittedAt());
    }
}
