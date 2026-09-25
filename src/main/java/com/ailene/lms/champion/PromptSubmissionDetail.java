package com.ailene.lms.champion;

import com.ailene.lms.common.CategorySummary;
import com.ailene.lms.prompt.PromptEvaluation;

import java.time.Instant;
import java.util.List;

public record PromptSubmissionDetail(Integer id, PromptSubject prompt, SubmissionMember member,
        SubmissionReviewer reviewedBy, Instant deadline, String message, String input, String output,
        Instant submittedAt, Instant reviewedAt, String comment, boolean isAccepted, PromptEvaluation evaluation,
        List<CategorySummary> categories) {
}
