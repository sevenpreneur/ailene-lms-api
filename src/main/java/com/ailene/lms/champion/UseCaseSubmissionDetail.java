package com.ailene.lms.champion;

import com.ailene.lms.common.CategorySummary;

import java.time.Instant;
import java.util.List;

public record UseCaseSubmissionDetail(Integer id, UseCaseSubject useCase, SubmissionMember member,
        SubmissionReviewer reviewedBy, Instant deadline, String message, String outcomeProof, Double hoursWithAi,
        Double hoursWithoutAi, String description, String aiTool, String frequency, String type,
        Instant submittedAt, Instant reviewedAt, String comment, boolean isAccepted,
        List<CategorySummary> categories) {
}
