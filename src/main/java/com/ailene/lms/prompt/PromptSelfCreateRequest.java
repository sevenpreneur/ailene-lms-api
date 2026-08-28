package com.ailene.lms.prompt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PromptSelfCreateRequest(@NotBlank String projectId, @NotBlank @Size(max = 255) String name,
        @NotBlank String scenario, @NotBlank @Size(max = 5000) String input,
        @NotBlank @Size(max = 10000) String output, @NotEmpty @Size(max = 2) List<Short> categoryIds) {
}
