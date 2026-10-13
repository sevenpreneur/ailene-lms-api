package com.ailene.lms.survey;

import java.time.OffsetDateTime;

public record ChapterSurveyStatus(Boolean available, Boolean submitted, OffsetDateTime submittedAt) {
}
