package com.ailene.lms.admin;

import com.ailene.lms.access.AccessRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMemberRequest(@NotBlank String projectId, @NotBlank String accessId, AccessRole role,
        Integer groupId, @Size(max = 255) String fullName, @Size(max = 255) String jobTitle) {
}
