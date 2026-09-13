package com.ailene.lms.champion;

import com.ailene.lms.common.CategorySummary;

import java.time.Instant;
import java.util.List;

public record PromptSubmissionDetail(Integer id, PromptSubject prompt, SubmissionMember member,
        SubmissionReviewer reviewedBy, Instant deadline, String message, String input, String output,
        Instant submittedAt, Instant reviewedAt, String comment, boolean isAccepted, Short rubricSpecificity,
        Short rubricContext, Short rubricConstraints, Short rubricExamples, Short rubricIteration,
        List<CategorySummary> categories) {
}
