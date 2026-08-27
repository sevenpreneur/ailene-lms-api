package com.ailene.lms.announcement;

import com.ailene.lms.common.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record AnnouncementUpsertRequest(@NotBlank String projectId, @NotBlank String title, String callout,
        @NotNull Status status, @NotNull OffsetDateTime startDate, @NotNull OffsetDateTime endDate) {
}
