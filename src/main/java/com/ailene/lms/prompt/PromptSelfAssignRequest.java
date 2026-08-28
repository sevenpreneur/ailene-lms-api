package com.ailene.lms.prompt;

import jakarta.validation.constraints.NotNull;

public record PromptSelfAssignRequest(@NotNull Integer promptId) {
}
