package com.ailene.lms.quiz;

import jakarta.validation.constraints.NotBlank;

public record QuizResultRequest(@NotBlank String quizId) {
}
