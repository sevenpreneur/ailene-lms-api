package com.ailene.lms.survey;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.access.StudentStatusProjection;
import com.ailene.lms.auth.AuthService;
import com.ailene.lms.chapter.Chapter;
import com.ailene.lms.chapter.ChapterRepository;
import com.ailene.lms.common.exception.ConflictException;
import com.ailene.lms.common.exception.ForbiddenException;
import com.ailene.lms.level.Level;
import com.ailene.lms.level.LevelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
    private LevelRepository levelRepository;
    @Mock
    private ChapterSurveyRepository chapterSurveyRepository;

    @InjectMocks
    private ChapterSurveyService chapterSurveyService;

    @BeforeEach
    void setUp() {
        when(authService.resolveUserId("jwt")).thenReturn(USER_ID);

        Chapter chapter = new Chapter();
        chapter.setId(5);
        chapter.setLevelId(1);
        chapter.setProjectId("proj-1");
        when(chapterRepository.findById(5)).thenReturn(Optional.of(chapter));

        Level level = new Level();
        level.setId(1);
        level.setLevelNumber((short) 1);
        when(levelRepository.findById(1)).thenReturn(Optional.of(level));

        Access access = new Access();
        access.setId("access-1");
        access.setProjectId("proj-1");
        when(accessRepository.findByUserIdAndProjectId(USER_ID, "proj-1")).thenReturn(Optional.of(access));

        StudentStatusProjection status = mock(StudentStatusProjection.class);
        when(status.getCurrentLevelNumber()).thenReturn((short) 1);
        when(accessRepository.findStudentStatus(USER_ID, "proj-1")).thenReturn(Optional.of(status));
    }

    private void givenSession(Instant sessionDate) {
        ChapterSessionSlotProjection session = mock(ChapterSessionSlotProjection.class);
        when(session.getSessionDate()).thenReturn(sessionDate);
        org.mockito.Mockito.lenient().when(session.getId()).thenReturn(77);
        when(chapterSurveyRepository.findApplicableSession(5, "access-1")).thenReturn(Optional.of(session));
    }

    private static ChapterSurveySubmitRequest request(String comment) {
        return new ChapterSurveySubmitRequest(5, (short) 5, (short) 4, (short) 3, comment);
    }

    @Test
    void refusedBeforeTheSessionStarts() {
        givenSession(Instant.now().plus(Duration.ofDays(2)));

        assertThatThrownBy(() -> chapterSurveyService.submit("jwt", request(null)))
                .isInstanceOf(ForbiddenException.class);
        verify(chapterSurveyRepository, never()).saveAndFlush(any());
    }

    @Test
    void secondSubmitIsAConflict() {
        givenSession(Instant.now().minus(Duration.ofHours(1)));
        when(chapterSurveyRepository.findByStudentAccessIdAndChapterId("access-1", 5))
                .thenReturn(Optional.of(new ChapterSurvey()));

        assertThatThrownBy(() -> chapterSurveyService.submit("jwt", request(null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void storesTheRatingsAgainstTheSessionAndDropsABlankComment() {
        givenSession(Instant.now().minus(Duration.ofHours(1)));
        when(chapterSurveyRepository.findByStudentAccessIdAndChapterId("access-1", 5)).thenReturn(Optional.empty());
        when(chapterSurveyRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        ChapterSurveyAnswer answer = chapterSurveyService.submit("jwt", request("   "));

        ArgumentCaptor<ChapterSurvey> saved = ArgumentCaptor.forClass(ChapterSurvey.class);
        verify(chapterSurveyRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getSessionId()).isEqualTo(77);
        assertThat(saved.getValue().getTrainerRating()).isEqualTo((short) 3);
        assertThat(answer.comment()).isNull();
        assertThat(answer.submittedAt()).isNotNull();
    }
}
