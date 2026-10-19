package com.ailene.lms.notification;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import com.ailene.lms.common.pagination.Pagination;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final AccessRepository accessRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationWriter notificationWriter;

    public NotificationListResponse list(UUID userId, NotificationListRequest request) {
        Access access = requireAccess(userId, request.projectId());
        notificationWriter.syncReminders(access);

        int page = Pagination.normalizePage(request.page());
        int pageSize = Pagination.normalizePageSize(request.pageSize());
        PageRequest pageable = PageRequest.of(page - 1, pageSize);
        Page<Notification> rows = Boolean.TRUE.equals(request.unreadOnly())
                ? notificationRepository.findByAccessIdAndReadAtIsNullOrderByCreatedAtDescIdDesc(access.getId(),
                        pageable)
                : notificationRepository.findByAccessIdOrderByCreatedAtDescIdDesc(access.getId(), pageable);

        return new NotificationListResponse(rows.getContent().stream().map(NotificationItem::from).toList(),
                Pagination.meta(rows.getTotalElements(), page, pageSize),
                notificationRepository.countByAccessIdAndReadAtIsNull(access.getId()));
    }

    public NotificationUnreadResponse unreadCount(UUID userId, NotificationProjectRequest request) {
        Access access = requireAccess(userId, request.projectId());
        notificationWriter.syncReminders(access);
        return new NotificationUnreadResponse(notificationRepository.countByAccessIdAndReadAtIsNull(access.getId()));
    }

    // Ids that are not the caller's, or already read, are skipped silently and not counted in updated.
    @Transactional
    public NotificationReadResponse markRead(UUID userId, NotificationReadRequest request) {
        Access access = requireAccess(userId, request.projectId());
        int updated = notificationRepository.markRead(access.getId(),
                request.notificationIds().stream().distinct().toList());
        return new NotificationReadResponse(updated,
                notificationRepository.countByAccessIdAndReadAtIsNull(access.getId()));
    }

    @Transactional
    public NotificationReadResponse markAllRead(UUID userId, NotificationProjectRequest request) {
        Access access = requireAccess(userId, request.projectId());
        int updated = notificationRepository.markAllRead(access.getId());
        return new NotificationReadResponse(updated, 0);
    }

    private Access requireAccess(UUID userId, String projectId) {
        return accessRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));
    }
}
