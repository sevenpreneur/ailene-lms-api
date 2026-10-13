package com.ailene.lms.chaptertest;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Map;

public record ChapterTestSubmitRequest(@NotNull @Positive Integer testId, @NotNull Map<String, String> answers) {
}
