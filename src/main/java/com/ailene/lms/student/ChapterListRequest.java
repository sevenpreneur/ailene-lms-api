package com.ailene.lms.student;

import jakarta.validation.constraints.NotBlank;

public record ChapterListRequest(@NotBlank String projectId) {
}
