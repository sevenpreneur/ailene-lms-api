package com.ailene.lms.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateGroupRequest(@NotBlank String projectId, @NotNull Integer groupId,
        @NotBlank @Size(max = 255) String name) {
}
