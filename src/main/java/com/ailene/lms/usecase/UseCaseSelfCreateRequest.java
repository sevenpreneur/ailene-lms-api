package com.ailene.lms.usecase;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record UseCaseSelfCreateRequest(@NotBlank String projectId, @NotBlank @Size(max = 255) String name,
        @NotEmpty @Size(max = 2) List<Short> categoryIds, @NotBlank @Size(max = 500) String outcomeProof,
        @NotNull @DecimalMin("0") @DecimalMax("9999.99") BigDecimal hoursWithAi,
        @NotNull @DecimalMin("0") @DecimalMax("9999.99") BigDecimal hoursWithoutAi,
        @NotBlank @Size(max = 5000) String description, @NotBlank @Size(max = 255) String aiTool,
        @NotNull UseCaseFrequency frequency, @NotNull UseCaseType type) {
}
