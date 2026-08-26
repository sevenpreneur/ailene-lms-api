package com.ailene.lms.quiz;

import jakarta.validation.constraints.NotBlank;

public record QuizDetailsRequest(@NotBlank String quizId) {
}
