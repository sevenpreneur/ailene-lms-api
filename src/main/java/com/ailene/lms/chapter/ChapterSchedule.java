package com.ailene.lms.chapter;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class ChapterSchedule {

    private static final Comparator<ChapterSession> GROUP_SESSION_FIRST = Comparator
            .comparing((ChapterSession s) -> s.getOnlyGroupId() == null)
            .thenComparing(ChapterSession::getId);

    private ChapterSchedule() {
    }

    // Same rule as the display queries: a group-specific session beats the all-groups one, lowest id breaks ties.
    public static Optional<ChapterSession> pick(List<ChapterSession> applicable) {
        return applicable.stream().min(GROUP_SESSION_FIRST);
    }

    public static ChapterLockedReason lockedReason(Boolean isLocked, Instant sessionDate, Instant now) {
        if (Boolean.TRUE.equals(isLocked)) {
            return ChapterLockedReason.admin_locked;
        }
        if (sessionDate == null || sessionDate.isAfter(now)) {
            return ChapterLockedReason.session_not_started;
        }
        return null;
    }

    public static ChapterLockedReason lockedReason(ChapterSession session, Instant now) {
        return lockedReason(session.getIsLocked(),
                session.getSessionDate() == null ? null : session.getSessionDate().toInstant(), now);
    }
}
