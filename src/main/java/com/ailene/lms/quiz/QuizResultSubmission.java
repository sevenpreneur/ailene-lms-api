package com.ailene.lms.quiz;

import java.time.OffsetDateTime;
import java.util.Map;

public record QuizResultSubmission(Short attemptNumber, Short score, Map<String, String> answers,
        OffsetDateTime submittedAt) {
}
