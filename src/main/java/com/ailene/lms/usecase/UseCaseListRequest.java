package com.ailene.lms.usecase;

import jakarta.validation.constraints.NotBlank;

public record UseCaseListRequest(@NotBlank String projectId, String search, Integer page, Integer pageSize) {
}
