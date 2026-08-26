package com.ailene.lms.learnings;

import java.time.OffsetDateTime;

public record MaterialCompletionResponse(String materialId, Boolean completed, OffsetDateTime completedAt,
        Short xpAwarded) {
}
