package com.ailene.lms.usecase;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UseCaseSubmitRequest(@NotNull Integer useCaseId, @NotBlank @Size(max = 500) String outcomeProof,
        @NotNull @DecimalMin("0") @DecimalMax("9999.99") BigDecimal hoursWithAi,
        @NotNull @DecimalMin("0") @DecimalMax("9999.99") BigDecimal hoursWithoutAi,
        @NotBlank @Size(max = 5000) String description, @NotBlank @Size(max = 255) String aiTool,
        @NotNull UseCaseFrequency frequency, @NotNull UseCaseType type) {
}
