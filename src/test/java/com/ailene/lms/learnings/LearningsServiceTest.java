package com.ailene.lms.learnings;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.access.StudentStatusProjection;
import com.ailene.lms.auth.AuthService;
import com.ailene.lms.chapter.Chapter;
import com.ailene.lms.chapter.ChapterAccessGuard;
import com.ailene.lms.chapter.ChapterListProjection;
import com.ailene.lms.chapter.ChapterLockedReason;
import com.ailene.lms.chapter.ChapterRepository;
import com.ailene.lms.chaptertest.ChapterTestService;
import com.ailene.lms.common.exception.ForbiddenException;
import com.ailene.lms.level.LevelRepository;
import com.ailene.lms.material.MaterialFocusProjection;
import com.ailene.lms.material.MaterialRepository;
import com.ailene.lms.prompt.PromptRepository;
import com.ailene.lms.quiz.QuizRepository;
import com.ailene.lms.survey.ChapterSurveyService;
import com.ailene.lms.usecase.UseCaseRepository;
import com.ailene.lms.video.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningsServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final Instant PAST = Instant.now().minus(Duration.ofDays(1));
    private static final Instant FUTURE = Instant.now().plus(Duration.ofDays(1));

    @Mock
    private AuthService authService;
    @Mock
    private AccessRepository accessRepository;
    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private LevelRepository levelRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private VideoRepository videoRepository;
    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private PromptRepository promptRepository;
    @Mock
    private UseCaseRepository useCaseRepository;
    @Mock
    private ChapterTestService chapterTestService;
    @Mock
    private ChapterSurveyService chapterSurveyService;
    @Mock
    private ChapterAccessGuard chapterAccessGuard;

    @InjectMocks
    private LearningsService learningsService;

    @BeforeEach
    void setUp() {
        when(authService.resolveUserId("jwt")).thenReturn(USER_ID);

        Access access = new Access();
        access.setId("access-1");
        access.setProjectId("proj-1");
        when(accessRepository.findByUserIdAndProjectId(USER_ID, "proj-1")).thenReturn(Optional.of(access));
    }

    private static ChapterListProjection chapter(int id, Instant sessionDate, Boolean sessionLocked) {
        ChapterListProjection row = mock(ChapterListProjection.class);
        lenient().when(row.getId()).thenReturn(id);
        lenient().when(row.getName()).thenReturn("Chapter " + id);
        lenient().when(row.getSessionDate()).thenReturn(sessionDate);
        lenient().when(row.getSessionLocked()).thenReturn(sessionLocked);
        lenient().when(row.getMethod()).thenReturn("offline");
        lenient().when(row.getLevelId()).thenReturn(1);
        lenient().when(row.getLevelNumber()).thenReturn((short) 1);
        lenient().when(row.getTotalTasks()).thenReturn(2L);
        lenient().when(row.getDoneTasks()).thenReturn(0L);
        return row;
    }

    private static MaterialFocusProjection material(int chapterId, String id) {
        MaterialFocusProjection row = mock(MaterialFocusProjection.class);
        lenient().when(row.getChapterId()).thenReturn(chapterId);
        lenient().when(row.getMaterialId()).thenReturn(id);
        lenient().when(row.getTitle()).thenReturn(id);
        lenient().when(row.getCompleted()).thenReturn(false);
        return row;
    }

    @Test
    void chaptersReportTheEffectiveLockPerCaller() {
        List<ChapterListProjection> rows = List.of(
                chapter(1, PAST, false), chapter(2, FUTURE, false), chapter(3, PAST, true));
        when(chapterRepository.findChapterList("proj-1", "access-1")).thenReturn(rows);

        List<ChapterListItem> chapters = learningsService.getChapters("jwt", new ChapterListRequest("proj-1"));

        assertThat(chapters).extracting(ChapterListItem::locked).containsExactly(false, true, true);
        assertThat(chapters).extracting(ChapterListItem::lockedReason).containsExactly(null,
                ChapterLockedReason.session_not_started, ChapterLockedReason.admin_locked);
        assertThat(chapters.get(1).sessionDate().toInstant()).isEqualTo(FUTURE);
    }

    @Test
    void taskOfALockedChapterIsForbidden() {
        Chapter chapter = new Chapter();
        chapter.setId(5);
        chapter.setProjectId("proj-1");
        when(chapterRepository.findById(5)).thenReturn(Optional.of(chapter));
        when(chapterAccessGuard.requireUnlocked(5, "access-1"))
                .thenThrow(new ForbiddenException(ChapterAccessGuard.message(ChapterLockedReason.admin_locked)));

        assertThatThrownBy(() -> learningsService.getTasks("jwt", new LearningsRequest(5)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("This chapter has been locked by an admin.");
        verify(materialRepository, never()).findMaterialTasks(anyInt(), anyString());
    }

    @Test
    void todayFocusSkipsAnAdminLockedSession() {
        StudentStatusProjection status = mock(StudentStatusProjection.class);
        when(status.getCurrentLevelNumber()).thenReturn((short) 1);
        when(accessRepository.findStudentStatus(USER_ID, "proj-1")).thenReturn(Optional.of(status));
        List<ChapterListProjection> chapters = List.of(chapter(1, PAST, true), chapter(2, PAST, false));
        List<MaterialFocusProjection> materials = List.of(material(1, "mat-locked"), material(2, "mat-open"));
        when(chapterRepository.findChapterList("proj-1", "access-1")).thenReturn(chapters);
        when(materialRepository.findProjectMaterialsForFocus("proj-1", "access-1")).thenReturn(materials);

        TodayFocusItem focus = learningsService.getTodayFocus("jwt", new TodayFocusRequest("proj-1")).focus();

        assertThat(focus.kind()).isEqualTo(TodayFocusKind.material);
        assertThat(focus.taskId()).isEqualTo("mat-open");
        assertThat(focus.chapterId()).isEqualTo(2);
    }

    @Test
    void todayFocusIsEmptyWhenEveryStartedSessionIsLocked() {
        StudentStatusProjection status = mock(StudentStatusProjection.class);
        when(status.getCurrentLevelNumber()).thenReturn((short) 1);
        when(accessRepository.findStudentStatus(USER_ID, "proj-1")).thenReturn(Optional.of(status));
        List<ChapterListProjection> chapters = List.of(chapter(1, PAST, true), chapter(2, FUTURE, false));
        List<MaterialFocusProjection> materials = List.of(material(1, "mat-locked"), material(2, "mat-future"));
        when(chapterRepository.findChapterList("proj-1", "access-1")).thenReturn(chapters);
        when(materialRepository.findProjectMaterialsForFocus("proj-1", "access-1")).thenReturn(materials);

        assertThat(learningsService.getTodayFocus("jwt", new TodayFocusRequest("proj-1")).focus()).isNull();
    }
}
