package com.ailene.lms.preassessment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PreAssessmentSubmitRequest(@NotBlank String projectId, @NotEmpty List<@Valid @NotNull Answer> answers) {

    public record Answer(@NotNull Integer itemId, Integer optionId, @Size(max = 4000) String text) {
    }
}
