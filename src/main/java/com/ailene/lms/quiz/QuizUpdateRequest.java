package com.ailene.lms.quiz;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record QuizUpdateRequest(@NotBlank String quizId, @NotNull Map<String, String> answers) {
}
