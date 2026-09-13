package com.ailene.lms.sponsor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SponsorGroupRequest(@NotBlank String projectId, @NotNull @Positive Integer groupId) {
}
