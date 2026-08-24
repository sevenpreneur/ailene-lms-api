package com.ailene.lms.learnings;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record LearningsRequest(@NotNull @Positive Integer chapterId) {
}
