package com.ailene.lms.learnings;

import jakarta.validation.constraints.NotBlank;

public record QuizDetailsRequest(@NotBlank String quizId) {
}
