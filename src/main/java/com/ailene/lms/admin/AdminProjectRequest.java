package com.ailene.lms.admin;

import jakarta.validation.constraints.NotBlank;

public record AdminProjectRequest(@NotBlank String projectId) {
}
