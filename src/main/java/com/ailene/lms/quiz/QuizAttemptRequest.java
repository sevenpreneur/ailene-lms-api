package com.ailene.lms.quiz;

import jakarta.validation.constraints.NotBlank;

public record QuizAttemptRequest(@NotBlank String quizId) {
}
