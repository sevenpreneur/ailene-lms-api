package com.ailene.lms.announcement;

import jakarta.validation.constraints.NotBlank;

public record AnnouncementDetailsRequest(@NotBlank String projectId) {
}
