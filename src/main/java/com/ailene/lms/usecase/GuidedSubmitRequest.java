package com.ailene.lms.usecase;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record GuidedSubmitRequest(@NotNull @Positive Integer sessionId, @Size(max = 255) String name,
        @Size(max = 2) List<@NotNull Short> categoryIds,
        @NotBlank @Size(max = 500) String outcomeProof,
        @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("9999.99")
        @Digits(integer = 4, fraction = 2) BigDecimal hoursWithAi,
        @NotBlank @Size(max = 5000) String description, @NotBlank @Size(max = 255) String aiTool,
        @NotNull UseCaseType type, @NotBlank @Size(min = 50, max = 5000) String testimonial,
        @NotNull @Min(1) @Max(5) Integer helpfulnessRating, @NotNull Boolean shareConsent) {
}
