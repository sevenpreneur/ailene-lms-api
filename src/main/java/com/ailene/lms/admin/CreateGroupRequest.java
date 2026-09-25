package com.ailene.lms.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGroupRequest(@NotBlank String projectId, @NotBlank @Size(max = 255) String name) {
}
