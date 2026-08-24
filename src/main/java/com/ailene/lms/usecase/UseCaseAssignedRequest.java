package com.ailene.lms.usecase;

import jakarta.validation.constraints.NotBlank;

public record UseCaseAssignedRequest(@NotBlank String projectId, Boolean hasSubmitted, Boolean isAccepted) {
}
