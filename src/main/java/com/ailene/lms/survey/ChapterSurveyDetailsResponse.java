package com.ailene.lms.survey;

import com.ailene.lms.common.ChapterSummary;

import java.time.OffsetDateTime;

public record ChapterSurveyDetailsResponse(ChapterSummary chapter, OffsetDateTime sessionDate, Boolean available,
        ChapterSurveyAnswer response) {
}
