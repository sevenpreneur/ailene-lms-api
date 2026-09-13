package com.ailene.lms.champion;

import jakarta.validation.constraints.NotBlank;

public record ChampionProjectRequest(@NotBlank String projectId) {
}
