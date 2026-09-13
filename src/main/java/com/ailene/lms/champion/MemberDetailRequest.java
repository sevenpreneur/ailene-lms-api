package com.ailene.lms.champion;

import jakarta.validation.constraints.NotBlank;

public record MemberDetailRequest(@NotBlank String projectId, @NotBlank String memberAccessId) {
}
