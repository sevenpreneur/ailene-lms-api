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
    void lockedBeforeTheSessionDate() {
        assertThat(ChapterSchedule.lockedReason(false, SESSION_DATE, SESSION_DATE.minusMillis(1)))
                .isEqualTo(ChapterLockedReason.session_not_started);
    }

    @Test
    void openExactlyAtTheSessionDate() {
        assertThat(ChapterSchedule.lockedReason(false, SESSION_DATE, SESSION_DATE)).isNull();
    }

    @Test
    void openAfterTheSessionDate() {
        assertThat(ChapterSchedule.lockedReason(false, SESSION_DATE, SESSION_DATE.plus(Duration.ofDays(3)))).isNull();
    }

    @Test
    void adminLockHoldsAfterTheSessionDate() {
        assertThat(ChapterSchedule.lockedReason(true, SESSION_DATE, SESSION_DATE.plus(Duration.ofDays(3))))
                .isEqualTo(ChapterLockedReason.admin_locked);
    }

    @Test
    void adminLockIsReportedOverAnUnstartedSession() {
        assertThat(ChapterSchedule.lockedReason(true, SESSION_DATE, SESSION_DATE.minusSeconds(60)))
                .isEqualTo(ChapterLockedReason.admin_locked);
    }

    @Test
    void sessionOverloadReadsTheEntity() {
        assertThat(ChapterSchedule.lockedReason(session(1, null, SESSION_DATE, false), SESSION_DATE)).isNull();
        assertThat(ChapterSchedule.lockedReason(session(1, null, SESSION_DATE, true), SESSION_DATE))
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
