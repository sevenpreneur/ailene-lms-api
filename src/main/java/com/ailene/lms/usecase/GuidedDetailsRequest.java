package com.ailene.lms.usecase;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record GuidedDetailsRequest(@Size(max = 21) String projectId, @Positive Integer useCaseId,
        @Positive Integer sessionId) {
}
