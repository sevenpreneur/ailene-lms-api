package com.ailene.lms.coaching;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CoachingNotesRequest(@NotBlank String projectId, @NotNull CoachingNoteRole role) {
}
