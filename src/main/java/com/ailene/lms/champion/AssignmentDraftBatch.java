package com.ailene.lms.champion;

import java.time.OffsetDateTime;
import java.util.List;

public record AssignmentDraftBatch(String batchId, String instruction, AssignmentKind requestedKind,
        OffsetDateTime createdAt, List<AssignmentDraftDto> drafts) {

    // Every draft in a batch shares its instruction, requested kind and insert time, so the first one speaks for all.
    public static AssignmentDraftBatch from(List<AssignmentDraft> drafts) {
        AssignmentDraft first = drafts.get(0);
        return new AssignmentDraftBatch(first.getBatchId(), first.getInstruction(), first.getRequestedKind(),
                first.getCreatedAt(), drafts.stream().map(AssignmentDraftDto::from).toList());
    }
}
