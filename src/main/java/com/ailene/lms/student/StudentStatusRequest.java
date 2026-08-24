package com.ailene.lms.student;

import jakarta.validation.constraints.NotBlank;

public record StudentStatusRequest(@NotBlank String projectId) {
}
