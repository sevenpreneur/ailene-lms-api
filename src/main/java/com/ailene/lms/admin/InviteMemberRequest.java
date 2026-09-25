package com.ailene.lms.admin;

import com.ailene.lms.access.AccessRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InviteMemberRequest(@NotBlank String projectId, @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 255) String fullName, @Size(max = 255) String jobTitle, @NotNull AccessRole role,
        @NotNull Integer groupId) {
}
