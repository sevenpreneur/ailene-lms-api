package com.ailene.lms.survey;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.auth.AuthService;
import com.ailene.lms.chapter.Chapter;
import com.ailene.lms.chapter.ChapterAccessGuard;
import com.ailene.lms.chapter.ChapterLockedReason;
import com.ailene.lms.chapter.ChapterRepository;
import com.ailene.lms.chapter.ChapterSession;
import com.ailene.lms.common.exception.ConflictException;
import com.ailene.lms.common.exception.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChapterSurveyServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private AuthService authService;
    @Mock
    private AccessRepository accessRepository;
    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private ChapterSurveyRepository chapterSurveyRepository;
    @Mock
    private ChapterAccessGuard chapterAccessGuard;

    @InjectMocks
    private ChapterSurveyService chapterSurveyService;

    private void givenCaller() {
        when(authService.resolveUserId("jwt")).thenReturn(USER_ID);

        Chapter chapter = new Chapter();
        chapter.setId(5);
        chapter.setLevelId(1);
        chapter.setProjectId("proj-1");
        when(chapterRepository.findById(5)).thenReturn(Optional.of(chapter));

        Access access = new Access();
        access.setId("access-1");
        access.setProjectId("proj-1");
        when(accessRepository.findByUserIdAndProjectId(USER_ID, "proj-1")).thenReturn(Optional.of(access));
    }

    private static ChapterSession session(Duration fromNow, boolean isLocked) {
        ChapterSession session = new ChapterSession();
        session.setId(77);
        session.setSessionDate(OffsetDateTime.now().plus(fromNow));
        session.setIsLocked(isLocked);
        return session;
    }

    private static ChapterSurveySubmitRequest request(String missingTopics) {
        return new ChapterSurveySubmitRequest(5, (short) 5, (short) 4, (short) 3, (short) 4, (short) 5, (short) 4,
                (short) 3, (short) 5, missingTopics, "  Studi kasusnya  ", null, "");
    }

    @Test
    void refusedWhileTheChapterIsLocked() {
        givenCaller();
        when(chapterAccessGuard.requireUnlocked(5, "access-1")).thenThrow(
                new ForbiddenException(ChapterAccessGuard.message(ChapterLockedReason.admin_locked)));

        assertThatThrownBy(() -> chapterSurveyService.submit("jwt", request(null)))
                .isInstanceOf(ForbiddenException.class);
        verify(chapterSurveyRepository, never()).saveAndFlush(any());
    }

    @Test
    void detailsOfALockedChapterAreForbidden() {
        givenCaller();
        when(chapterAccessGuard.requireUnlocked(5, "access-1"))
                .thenThrow(new ForbiddenException(ChapterAccessGuard.message(ChapterLockedReason.admin_locked)));

        assertThatThrownBy(() -> chapterSurveyService.getDetails("jwt", new ChapterSurveyDetailsRequest(5)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void secondSubmitIsAConflict() {
        givenCaller();
        when(chapterAccessGuard.requireUnlocked(5, "access-1")).thenReturn(session(Duration.ofHours(-1), false));
        when(chapterSurveyRepository.findByStudentAccessIdAndChapterId("access-1", 5))
                .thenReturn(Optional.of(new ChapterSurvey()));

        assertThatThrownBy(() -> chapterSurveyService.submit("jwt", request(null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void storesTheRatingsAgainstTheSessionAndDropsBlankAnswers() {
        givenCaller();
        when(chapterAccessGuard.requireUnlocked(5, "access-1")).thenReturn(session(Duration.ofHours(-1), false));
        when(chapterSurveyRepository.findByStudentAccessIdAndChapterId("access-1", 5)).thenReturn(Optional.empty());
        when(chapterSurveyRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        ChapterSurveyAnswer answer = chapterSurveyService.submit("jwt", request("   "));

        ArgumentCaptor<ChapterSurvey> saved = ArgumentCaptor.forClass(ChapterSurvey.class);
        verify(chapterSurveyRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getSessionId()).isEqualTo(77);
        assertThat(saved.getValue().getTrainerResponsiveness()).isEqualTo((short) 3);
        assertThat(saved.getValue().getLearningValue()).isEqualTo((short) 5);
        assertThat(answer.missingTopics()).isNull();
        assertThat(answer.favoriteMaterial()).isEqualTo("Studi kasusnya");
        assertThat(answer.dislikedMaterial()).isNull();
        assertThat(answer.improvementSuggestion()).isNull();
        assertThat(answer.submittedAt()).isNotNull();
    }

    @Test
    void statusIsUnavailableWhileTheSessionIsAdminLocked() {
        when(chapterAccessGuard.findSession(5, "access-1"))
                .thenReturn(Optional.of(session(Duration.ofHours(-1), true)));
        when(chapterSurveyRepository.findByStudentAccessIdAndChapterId("access-1", 5)).thenReturn(Optional.empty());

        assertThat(chapterSurveyService.findStatus(5, "access-1").available()).isFalse();
    }

    @Test
    void statusIsAvailableForAnUnlockedSessionEvenBeforeItsDate() {
        when(chapterAccessGuard.findSession(5, "access-1"))
                .thenReturn(Optional.of(session(Duration.ofHours(1), false)));
        when(chapterSurveyRepository.findByStudentAccessIdAndChapterId("access-1", 5)).thenReturn(Optional.empty());

        assertThat(chapterSurveyService.findStatus(5, "access-1").available()).isTrue();
    }
}
