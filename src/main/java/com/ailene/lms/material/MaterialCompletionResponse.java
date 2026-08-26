package com.ailene.lms.material;

import java.time.OffsetDateTime;

public record MaterialCompletionResponse(String materialId, Boolean completed, OffsetDateTime completedAt,
        Short xpAwarded) {
}
