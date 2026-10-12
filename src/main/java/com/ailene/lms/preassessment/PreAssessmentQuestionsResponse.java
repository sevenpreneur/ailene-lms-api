package com.ailene.lms.preassessment;

import java.time.OffsetDateTime;
import java.util.List;

public record PreAssessmentQuestionsResponse(Form form, Attempt attempt, List<Item> items) {

    public record Form(Integer id, String code, String name, Integer durationMinutes) {

        public static Form from(PreAssessmentFormProjection p) {
            return new Form(p.getId(), p.getCode(), p.getName(), p.getDurationMinutes());
        }
    }

    public record Attempt(Integer id, PreAssessmentAttemptStatus status, OffsetDateTime submittedAt) {

        public static Attempt from(PreAssessmentAttempt attempt) {
            return new Attempt(attempt.getId(), attempt.getStatus(), attempt.getSubmittedAt());
        }
    }

    public record Item(Integer id, String itemCode, String section, String type, String question,
            List<Option> options) {
    }

    public record Option(Integer id, String text) {
    }
}
