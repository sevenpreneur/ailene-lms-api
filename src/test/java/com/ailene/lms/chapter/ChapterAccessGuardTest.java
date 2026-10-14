package com.ailene.lms.chapter;

import com.ailene.lms.common.exception.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.ailene.lms.chapter.ChapterScheduleTest.session;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChapterAccessGuardTest {

    private static final Instant PAST = Instant.now().minus(Duration.ofDays(1));
    private static final Instant FUTURE = Instant.now().plus(Duration.ofDays(1));

    @Mock
    private ChapterSessionRepository chapterSessionRepository;

    @InjectMocks
    private ChapterAccessGuard chapterAccessGuard;

    private void givenSessions(ChapterSession... sessions) {
        when(chapterSessionRepository.findApplicableSessions(5, "access-1")).thenReturn(List.of(sessions));
    }

    @Test
    void noApplicableSessionIsForbidden() {
        givenSessions();

        assertThatThrownBy(() -> chapterAccessGuard.requireUnlocked(5, "access-1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("This chapter isn't scheduled for your group.");
    }

    @Test
    void unstartedSessionIsForbidden() {
        givenSessions(session(1, null, FUTURE, false));

        assertThatThrownBy(() -> chapterAccessGuard.requireUnlocked(5, "access-1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("This chapter opens when its session starts.");
    }

    @Test
    void adminLockedSessionIsForbiddenEvenAfterItStarted() {
        givenSessions(session(1, null, PAST, true));

        assertThatThrownBy(() -> chapterAccessGuard.requireUnlocked(5, "access-1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("This chapter has been locked by an admin.");
    }

    @Test
    void startedUnlockedSessionIsReturned() {
        ChapterSession open = session(1, null, PAST, false);
        givenSessions(open);

        assertThat(chapterAccessGuard.requireUnlocked(5, "access-1")).isSameAs(open);
    }

    @Test
    void lockedGroupSessionWinsOverAnOpenAllGroupsSession() {
        givenSessions(session(1, null, PAST, false), session(2, 7, PAST, true));

        assertThatThrownBy(() -> chapterAccessGuard.requireUnlocked(5, "access-1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("This chapter has been locked by an admin.");
    }

    @Test
    void unstartedGroupSessionWinsOverAStartedAllGroupsSession() {
        givenSessions(session(1, null, PAST, false), session(2, 7, FUTURE, false));

        assertThatThrownBy(() -> chapterAccessGuard.requireUnlocked(5, "access-1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("This chapter opens when its session starts.");
    }

    @Test
    void openGroupSessionWinsOverALockedAllGroupsSession() {
        ChapterSession ownGroup = session(2, 7, PAST, false);
        givenSessions(session(1, null, PAST, true), ownGroup);

        assertThat(chapterAccessGuard.requireUnlocked(5, "access-1")).isSameAs(ownGroup);
    }
}
