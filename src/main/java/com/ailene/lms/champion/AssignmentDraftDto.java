package com.ailene.lms.champion;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

public record AssignmentDraftDto(Integer id, AssignmentKind kind, String angle, String name, String description,
        String expectedOutput, List<Short> categoryIds, OffsetDateTime usedAt, Integer usedPromptId,
        Integer usedUseCaseId) {

    public static AssignmentDraftDto from(AssignmentDraft draft) {
        return new AssignmentDraftDto(draft.getId(), draft.getKind(), draft.getAngle(), draft.getName(),
                draft.getDescription(), draft.getExpectedOutput(), Arrays.asList(draft.getCategoryIds()),
                draft.getUsedAt(), draft.getUsedPromptId(), draft.getUsedUseCaseId());
    }
}
