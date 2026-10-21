package com.ailene.lms.chapter;

import com.ailene.lms.common.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ChapterAccessGuard {

    static final String NOT_SCHEDULED = "This chapter isn't scheduled for your group.";
    static final String ADMIN_LOCKED = "This chapter is locked until an admin opens it.";

    private final ChapterSessionRepository chapterSessionRepository;

    public Optional<ChapterSession> findSession(Integer chapterId, String accessId) {
        return ChapterSchedule.pick(chapterSessionRepository.findApplicableSessions(chapterId, accessId));
    }

    public ChapterSession requireUnlocked(Integer chapterId, String accessId) {
        // Keep an admin's curriculum replacement from racing a learner's scoring/submission transaction.
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && !TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            chapterSessionRepository.lockChapterForContentWrite(chapterId);
        }
        ChapterSession session = findSession(chapterId, accessId)
                .orElseThrow(() -> new ForbiddenException(NOT_SCHEDULED));
        ChapterLockedReason reason = ChapterSchedule.lockedReason(session);
        if (reason != null) {
            throw new ForbiddenException(message(reason));
        }
        return session;
    }

    public static String message(ChapterLockedReason reason) {
        return switch (reason) {
            case admin_locked -> ADMIN_LOCKED;
        };
    }
}
