package com.ailene.lms.chapter;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChapterScheduleTest {

    private static final Instant SESSION_DATE = Instant.parse("2026-10-20T02:00:00Z");

    static ChapterSession session(int id, Integer onlyGroupId, Instant sessionDate, boolean isLocked) {
        ChapterSession session = new ChapterSession();
        session.setId(id);
        session.setOnlyGroupId(onlyGroupId);
        session.setSessionDate(sessionDate.atOffset(ZoneOffset.UTC));
        session.setIsLocked(isLocked);
        return session;
    }

    @Test
    void lockedWhileIsLockedIsSet() {
        assertThat(ChapterSchedule.lockedReason(true)).isEqualTo(ChapterLockedReason.admin_locked);
    }

    @Test
    void openOnceIsLockedIsCleared() {
        assertThat(ChapterSchedule.lockedReason(false)).isNull();
    }

    @Test
    void missingFlagCountsAsLocked() {
        assertThat(ChapterSchedule.lockedReason((Boolean) null)).isEqualTo(ChapterLockedReason.admin_locked);
    }

    @Test
    void sessionDateNeverLocksOrOpens() {
        Instant future = Instant.now().plus(Duration.ofDays(30));
        Instant past = Instant.now().minus(Duration.ofDays(30));

        assertThat(ChapterSchedule.lockedReason(session(1, null, future, false))).isNull();
        assertThat(ChapterSchedule.lockedReason(session(1, null, past, true)))
                .isEqualTo(ChapterLockedReason.admin_locked);
    }

    @Test
    void groupSpecificSessionBeatsTheAllGroupsOne() {
        ChapterSession allGroups = session(1, null, SESSION_DATE, false);
        ChapterSession ownGroup = session(2, 7, SESSION_DATE.plus(Duration.ofDays(7)), true);

        assertThat(ChapterSchedule.pick(List.of(allGroups, ownGroup))).containsSame(ownGroup);
        assertThat(ChapterSchedule.pick(List.of(ownGroup, allGroups))).containsSame(ownGroup);
    }

    @Test
    void allGroupsSessionAppliesWhenTheGroupHasNoneOfItsOwn() {
        ChapterSession allGroups = session(1, null, SESSION_DATE, false);

        assertThat(ChapterSchedule.pick(List.of(allGroups))).containsSame(allGroups);
    }

    @Test
    void lowestIdBreaksATie() {
        ChapterSession later = session(9, null, SESSION_DATE, true);
        ChapterSession earlier = session(3, null, SESSION_DATE, false);

        assertThat(ChapterSchedule.pick(List.of(later, earlier))).containsSame(earlier);
    }

    @Test
    void noApplicableSessionPicksNothing() {
        assertThat(ChapterSchedule.pick(List.of())).isEmpty();
    }
}
