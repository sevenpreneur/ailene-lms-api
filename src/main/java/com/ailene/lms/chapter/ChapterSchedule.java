package com.ailene.lms.chapter;

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

    // Only the admin's is_locked decides; session_date is informational and never opens or closes a chapter.
    public static ChapterLockedReason lockedReason(Boolean isLocked) {
        return Boolean.FALSE.equals(isLocked) ? null : ChapterLockedReason.admin_locked;
    }

    public static ChapterLockedReason lockedReason(ChapterSession session) {
        return lockedReason(session.getIsLocked());
    }
}
