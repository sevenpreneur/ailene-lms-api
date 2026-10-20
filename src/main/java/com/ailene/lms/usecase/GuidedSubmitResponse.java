package com.ailene.lms.usecase;

import java.math.BigDecimal;

public record GuidedSubmitResponse(GuidedSessionResponse session, Integer submissionId, Integer useCaseId,
        String submissionStatus, BigDecimal hoursSaved) {
}
