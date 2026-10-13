package com.ailene.lms.survey;

import java.time.OffsetDateTime;

public record ChapterSurveyAnswer(Short satisfaction, Short relevance, Short trainerRating, String comment,
        OffsetDateTime submittedAt) {

    public static ChapterSurveyAnswer from(ChapterSurvey survey) {
        return new ChapterSurveyAnswer(survey.getSatisfaction(), survey.getRelevance(), survey.getTrainerRating(),
                survey.getComment(), survey.getSubmittedAt());
    }
}
