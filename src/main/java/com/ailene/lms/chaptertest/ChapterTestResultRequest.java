package com.ailene.lms.chaptertest;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ChapterTestResultRequest(@NotNull @Positive Integer testId) {
}
