package com.ailene.lms.prompt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PromptSubmitRequest(@NotNull Integer promptId, @NotBlank @Size(max = 5000) String input,
        @NotBlank @Size(max = 10000) String output) {
}
