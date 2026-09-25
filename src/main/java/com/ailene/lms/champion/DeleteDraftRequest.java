package com.ailene.lms.champion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeleteDraftRequest(@NotBlank String projectId, @NotNull Integer draftId) {
}
