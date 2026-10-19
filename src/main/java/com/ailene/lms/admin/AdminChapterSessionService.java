package com.ailene.lms.admin;

import com.ailene.lms.chapter.ChapterSession;
import com.ailene.lms.chapter.ChapterSessionRepository;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import com.ailene.lms.notification.NotificationWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminChapterSessionService {

    private final AdminGuard adminGuard;
    private final ChapterSessionRepository chapterSessionRepository;
    private final NotificationWriter notificationWriter;

    public List<AdminChapterSessionDto> listSessions(String token, AdminProjectRequest request) {
        adminGuard.requireAdminOnProject(token, request.projectId());
        return chapterSessionRepository.findByProjectIdOrderBySessionDateAscIdAsc(request.projectId()).stream()
                .map(AdminChapterSessionDto::from)
                .toList();
    }

    // Only this one row changes, so locking a group-specific session never touches another group's schedule.
    public AdminChapterSessionDto updateLock(String token, UpdateChapterSessionLockRequest request) {
        adminGuard.requireAdminOnProject(token, request.projectId());
        ChapterSession session = chapterSessionRepository.findByIdAndProjectId(request.sessionId(),
                request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("Chapter session not found"));

        session.setIsLocked(request.isLocked());
        session.setUpdatedAt(OffsetDateTime.now());
        ChapterSession saved = chapterSessionRepository.save(session);
        // Deduplicated per session, so locking and reopening never notifies the same member twice.
        if (!Boolean.TRUE.equals(saved.getIsLocked())) {
            notificationWriter.chapterUnlocked(saved.getId());
        }
        return AdminChapterSessionDto.from(saved);
    }
}
