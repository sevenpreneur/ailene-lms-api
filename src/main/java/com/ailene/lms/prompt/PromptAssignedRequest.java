package com.ailene.lms.prompt;

import jakarta.validation.constraints.NotBlank;

public record PromptAssignedRequest(@NotBlank String projectId, Boolean hasSubmitted, Boolean isAccepted) {
}
