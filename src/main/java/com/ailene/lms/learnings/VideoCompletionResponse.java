package com.ailene.lms.learnings;

import java.time.OffsetDateTime;

public record VideoCompletionResponse(Integer videoId, Boolean completed, OffsetDateTime completedAt,
        Short xpAwarded) {
}
