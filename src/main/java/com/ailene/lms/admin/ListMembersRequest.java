package com.ailene.lms.admin;

import jakarta.validation.constraints.NotBlank;

public record ListMembersRequest(@NotBlank String projectId, Integer groupId) {
}
