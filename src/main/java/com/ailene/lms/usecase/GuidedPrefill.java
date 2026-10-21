package com.ailene.lms.usecase;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record GuidedPrefill(@NotBlank @Size(min = 30, max = 3000) String dailyTask, @NotNull UseCaseFrequency frequency,
        @NotBlank @Size(max = 10000) String exampleText, @Size(max = 500) String exampleFileUrl,
        @NotNull @AssertTrue Boolean sensitiveDataConfirmed,
        @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("599999.40") @Digits(integer = 6, fraction = 2)
        BigDecimal minutesWithoutAi,
        @NotBlank @Size(max = 5000) String currentProcess, @Size(max = 20) List<@NotBlank @Size(max = 100) String> currentTools,
        @Size(max = 2000) String painPoint) {
}
