package com.ailene.lms.quiz;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record QuizSubmitRequest(@NotBlank String quizId, @NotNull Map<String, String> answers) {
}
