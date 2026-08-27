package com.ailene.lms.announcement;

import com.ailene.lms.common.Status;

import java.time.OffsetDateTime;

public record AnnouncementResponse(Integer id, String projectId, String title, String callout, Status status,
        OffsetDateTime startDate, OffsetDateTime endDate, OffsetDateTime updatedAt) {

    public static AnnouncementResponse from(Announcement announcement) {
        return new AnnouncementResponse(announcement.getId(), announcement.getProjectId(), announcement.getTitle(),
                announcement.getCallout(), announcement.getStatus(), announcement.getStartDate(),
                announcement.getEndDate(), announcement.getUpdatedAt());
    }
}
