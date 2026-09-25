package com.ailene.lms.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeleteGroupRequest(@NotBlank String projectId, @NotNull Integer groupId) {
}
