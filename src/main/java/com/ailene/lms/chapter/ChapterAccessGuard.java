package com.ailene.lms.chapter;

import com.ailene.lms.common.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ChapterAccessGuard {

    static final String NOT_SCHEDULED = "This chapter isn't scheduled for your group.";
    static final String SESSION_NOT_STARTED = "This chapter opens when its session starts.";
    static final String ADMIN_LOCKED = "This chapter has been locked by an admin.";

    private final ChapterSessionRepository chapterSessionRepository;

    public Optional<ChapterSession> findSession(Integer chapterId, String accessId) {
        return ChapterSchedule.pick(chapterSessionRepository.findApplicableSessions(chapterId, accessId));
    }

    public ChapterSession requireUnlocked(Integer chapterId, String accessId) {
        ChapterSession session = findSession(chapterId, accessId)
                .orElseThrow(() -> new ForbiddenException(NOT_SCHEDULED));
        ChapterLockedReason reason = ChapterSchedule.lockedReason(session, Instant.now());
        if (reason != null) {
            throw new ForbiddenException(message(reason));
        }
        return session;
    }

    public static String message(ChapterLockedReason reason) {
        return switch (reason) {
            case session_not_started -> SESSION_NOT_STARTED;
            case admin_locked -> ADMIN_LOCKED;
        };
    }
}
