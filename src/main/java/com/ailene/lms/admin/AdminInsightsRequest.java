package com.ailene.lms.admin;

import jakarta.validation.constraints.NotBlank;

// A null groupId reads the whole project.
public record AdminInsightsRequest(@NotBlank String projectId, Integer groupId) {
}
