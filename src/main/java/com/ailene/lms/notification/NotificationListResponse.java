package com.ailene.lms.notification;

import com.ailene.lms.common.pagination.PageMeta;

import java.util.List;

public record NotificationListResponse(List<NotificationItem> list, PageMeta metapaging, long unreadCount) {
}
