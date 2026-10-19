package com.ailene.lms.notification;

import java.time.OffsetDateTime;

public record NotificationItem(Integer id, NotificationType type, String title, String body, String refId,
        OffsetDateTime eventAt, OffsetDateTime readAt, OffsetDateTime createdAt) {

    public static NotificationItem from(Notification notification) {
        return new NotificationItem(notification.getId(), notification.getType(), notification.getTitle(),
                notification.getBody(), notification.getRefId(), notification.getEventAt(),
                notification.getReadAt(), notification.getCreatedAt());
    }
}
