package com.ailene.lms.quiz;

import java.time.OffsetDateTime;
import java.util.Map;

public record QuizAttemptResponse(String status, Integer submissionId, OffsetDateTime startedAt,
        OffsetDateTime serverNow, Integer secondsLeft, Map<String, String> answers) {

    public static QuizAttemptResponse active(Integer submissionId, OffsetDateTime startedAt,
            OffsetDateTime serverNow, int secondsLeft, Map<String, String> answers) {
        return new QuizAttemptResponse("active", submissionId, startedAt, serverNow, secondsLeft, answers);
    }

    public static QuizAttemptResponse finalized() {
        return new QuizAttemptResponse("finalized", null, null, null, null, null);
    }
}
