package com.ailene.lms.admin;

import jakarta.validation.constraints.NotBlank;

public record DeleteMemberRequest(@NotBlank String projectId, @NotBlank String accessId) {
}
