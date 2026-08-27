package com.ailene.lms.coaching;

import jakarta.validation.constraints.NotBlank;

public record CoachingNoteCreateRequest(@NotBlank String projectId, @NotBlank String studentAccessId,
        @NotBlank String text) {
}
