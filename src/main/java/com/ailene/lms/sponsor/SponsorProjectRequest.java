package com.ailene.lms.sponsor;

import jakarta.validation.constraints.NotBlank;

public record SponsorProjectRequest(@NotBlank String projectId) {
}
