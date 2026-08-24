package com.ailene.lms.prompt;

import jakarta.validation.constraints.NotBlank;

public record PromptListRequest(@NotBlank String projectId, String search, Integer page, Integer pageSize) {
}
