package com.ailene.lms.champion;

import jakarta.validation.constraints.NotBlank;

public record ChampionMembersRequest(@NotBlank String projectId, Integer groupId) {
}
