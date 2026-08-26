package com.ailene.lms.student;

import jakarta.validation.constraints.NotBlank;

public record LeaderboardRequest(@NotBlank String projectId) {
}
