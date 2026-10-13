package com.ailene.lms.survey;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ChapterSurveyDetailsRequest(@NotNull @Positive Integer chapterId) {
}
