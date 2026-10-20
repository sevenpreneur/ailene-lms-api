package com.ailene.lms.usecase;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record GuidedSaveRequest(@Size(max = 21) String projectId, @Positive Integer useCaseId,
        @Positive Integer sessionId, @NotNull @Min(1) @Max(6) Integer currentStep,
        @Size(max = 3000) String dailyTask, UseCaseFrequency frequency,
        @Size(max = 10000) String exampleText, @Size(max = 500) String exampleFileUrl,
        Boolean sensitiveDataConfirmed,
        @DecimalMin(value = "0", inclusive = false) @DecimalMax("599999.40") @Digits(integer = 6, fraction = 2)
        BigDecimal minutesWithoutAi,
        @Size(max = 5000) String currentProcess, @Size(max = 20) List<@NotBlank @Size(max = 100) String> currentTools,
        @Size(max = 2000) String painPoint) {
}
