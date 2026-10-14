package com.ailene.lms.chaptertest;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.auth.AuthService;
import com.ailene.lms.chapter.Chapter;
import com.ailene.lms.chapter.ChapterAccessGuard;
import com.ailene.lms.chapter.ChapterLockedReason;
import com.ailene.lms.chapter.ChapterRepository;
import com.ailene.lms.common.Status;
import com.ailene.lms.common.exception.ConflictException;
import com.ailene.lms.common.exception.ForbiddenException;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChapterTestServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private AuthService authService;
    @Mock
    private AccessRepository accessRepository;
    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private ChapterTestRepository chapterTestRepository;
    @Mock
    private ChapterTestSubmissionRepository chapterTestSubmissionRepository;
    @Mock
    private ChapterAccessGuard chapterAccessGuard;

    @InjectMocks
    private ChapterTestService chapterTestService;

    @BeforeEach
    void setUp() {
        when(authService.resolveUserId("jwt")).thenReturn(USER_ID);

        Chapter chapter = new Chapter();
        chapter.setId(5);
        chapter.setLevelId(1);
        chapter.setProjectId("proj-1");
        chapter.setName("Paham AI");
        lenient().when(chapterRepository.findById(5)).thenReturn(Optional.of(chapter));

        Access access = new Access();
        access.setId("access-1");
        access.setProjectId("proj-1");
        lenient().when(accessRepository.findByUserIdAndProjectId(USER_ID, "proj-1")).thenReturn(Optional.of(access));
    }

    private void givenTest(int id, ChapterTestKind kind) {
        ChapterTest test = new ChapterTest();
        test.setId(id);
        test.setChapterId(5);
        test.setKind(kind);
        test.setName(kind.name());
        test.setStatus(Status.active);
        when(chapterTestRepository.findById(id)).thenReturn(Optional.of(test));
    }

    private void givenChapterTests(ChapterTestTaskProjection... rows) {
        when(chapterTestRepository.findChapterTests(5, "access-1")).thenReturn(List.of(rows));
    }

    private void givenQuestions(int testId, ChapterTestQuestionOptionProjection... rows) {
        when(chapterTestRepository.findQuestionsWithOptions(testId)).thenReturn(List.of(rows));
    }

    private static ChapterTestTaskProjection row(int id, ChapterTestKind kind, Short score, boolean submitted) {
        ChapterTestTaskProjection row = mock(ChapterTestTaskProjection.class);
        lenient().when(row.getId()).thenReturn(id);
        lenient().when(row.getKind()).thenReturn(kind.name());
        lenient().when(row.getScore()).thenReturn(score);
        lenient().when(row.getSubmittedAt()).thenReturn(submitted ? Instant.now() : null);
        return row;
    }

    private static ChapterTestQuestionOptionProjection option(int questionId, String type, Integer optionId,
            String code, Boolean correct) {
        ChapterTestQuestionOptionProjection row = mock(ChapterTestQuestionOptionProjection.class);
        lenient().when(row.getQuestionId()).thenReturn(questionId);
        lenient().when(row.getType()).thenReturn(type);
        lenient().when(row.getQuestion()).thenReturn("Q" + questionId);
        lenient().when(row.getExplanation()).thenReturn("because");
        lenient().when(row.getOptionId()).thenReturn(optionId);
        lenient().when(row.getOptionCode()).thenReturn(code);
        lenient().when(row.getOptionText()).thenReturn(code);
        lenient().when(row.getOptionIsCorrect()).thenReturn(correct);
        return row;
    }

    private void givenChapterLocked(ChapterLockedReason reason) {
        when(chapterAccessGuard.requireUnlocked(5, "access-1"))
                .thenThrow(new ForbiddenException(ChapterAccessGuard.message(reason)));
    }

    @Test
    void detailsOfALockedChapterAreForbidden() {
        givenTest(10, ChapterTestKind.pre_test);
        givenChapterLocked(ChapterLockedReason.session_not_started);

        assertThatThrownBy(() -> chapterTestService.getDetails("jwt", new ChapterTestDetailsRequest(10)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("This chapter opens when its session starts.");
    }

    @Test
    void submitToALockedChapterIsForbiddenAndStoresNothing() {
        givenTest(10, ChapterTestKind.pre_test);
        givenChapterLocked(ChapterLockedReason.admin_locked);

        assertThatThrownBy(() -> chapterTestService.submit("jwt",
                new ChapterTestSubmitRequest(10, Map.of("1", "A"))))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("This chapter has been locked by an admin.");
        verify(chapterTestSubmissionRepository, never()).saveAndFlush(any());
    }

    @Test
    void resultOfALockedChapterIsForbidden() {
        givenTest(10, ChapterTestKind.pre_test);
        givenChapterLocked(ChapterLockedReason.admin_locked);

        assertThatThrownBy(() -> chapterTestService.getResult("jwt", new ChapterTestResultRequest(10)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void postTestIsRefusedUntilThePreTestIsDone() {
        givenTest(20, ChapterTestKind.post_test);
        givenChapterTests(
                row(10, ChapterTestKind.pre_test, null, false), row(20, ChapterTestKind.post_test, null, false));
        lenient().when(chapterTestRepository.countIncompleteMaterials(5, "access-1")).thenReturn(0L);

        assertThatThrownBy(() -> chapterTestService.submit("jwt",
                new ChapterTestSubmitRequest(20, Map.of("1", "A"))))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Finish this chapter's pre-test first.");
        verify(chapterTestSubmissionRepository, never()).saveAndFlush(any());
    }

    @Test
    void secondSubmitIsAConflict() {
        givenTest(10, ChapterTestKind.pre_test);
        givenChapterTests(row(10, ChapterTestKind.pre_test, (short) 50, true));

        assertThatThrownBy(() -> chapterTestService.submit("jwt",
                new ChapterTestSubmitRequest(10, Map.of("1", "A"))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void submitStoresScoreAndNormalisedAnswers() {
        givenTest(10, ChapterTestKind.pre_test);
        givenChapterTests(row(10, ChapterTestKind.pre_test, null, false));
        givenQuestions(10,
                option(1, "multiple_choice", 11, "A", true),
                option(1, "multiple_choice", 12, "B", false),
                option(2, "essay", null, null, null));
        when(chapterTestSubmissionRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        ChapterTestSubmitResponse response = chapterTestService.submit("jwt",
                new ChapterTestSubmitRequest(10, Map.of("1", "A", "2", " jawaban ")));

        assertThat(response.score()).isEqualTo((short) 100);
        assertThat(response.essayCount()).isEqualTo((short) 1);
        ArgumentCaptor<ChapterTestSubmission> saved = ArgumentCaptor.forClass(ChapterTestSubmission.class);
        verify(chapterTestSubmissionRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getAnswers()).containsEntry("2", "jawaban");
        assertThat(saved.getValue().getStudentAccessId()).isEqualTo("access-1");
    }

    @Test
    void preTestResultHidesTheKeyWhileThePostTestIsAhead() {
        givenTest(10, ChapterTestKind.pre_test);
        ChapterTestSubmission submission = new ChapterTestSubmission();
        submission.setAnswers(Map.of("1", "B"));
        submission.setScore((short) 0);
        when(chapterTestSubmissionRepository.findByStudentAccessIdAndTestId("access-1", 10))
                .thenReturn(Optional.of(submission));
        givenChapterTests(
                row(10, ChapterTestKind.pre_test, (short) 0, true), row(20, ChapterTestKind.post_test, null, false));
        givenQuestions(10,
                option(1, "multiple_choice", 11, "A", true), option(1, "multiple_choice", 12, "B", false));

        ChapterTestResultResponse result = chapterTestService.getResult("jwt", new ChapterTestResultRequest(10));

        assertThat(result.answersRevealed()).isFalse();
        ChapterTestResultQuestionItem question = result.questions().getFirst();
        assertThat(question.answer()).isEqualTo("B");
        assertThat(question.isCorrect()).isNull();
        assertThat(question.explanation()).isNull();
        assertThat(question.options()).allSatisfy(o -> assertThat(o.isCorrect()).isNull());
    }

    @Test
    void postTestResultCarriesThePreTestScore() {
        givenTest(20, ChapterTestKind.post_test);
        ChapterTestSubmission submission = new ChapterTestSubmission();
        submission.setAnswers(Map.of("1", "A"));
        submission.setScore((short) 100);
        when(chapterTestSubmissionRepository.findByStudentAccessIdAndTestId("access-1", 20))
                .thenReturn(Optional.of(submission));
        givenChapterTests(
                row(10, ChapterTestKind.pre_test, (short) 40, true),
                row(20, ChapterTestKind.post_test, (short) 100, true));
        givenQuestions(20,
                option(1, "multiple_choice", 11, "A", true), option(1, "multiple_choice", 12, "B", false));

        ChapterTestResultResponse result = chapterTestService.getResult("jwt", new ChapterTestResultRequest(20));

        assertThat(result.answersRevealed()).isTrue();
        assertThat(result.preTestScore()).isEqualTo((short) 40);
        assertThat(result.questions().getFirst().isCorrect()).isTrue();
    }

    @Test
    void inactiveTestIsNotFound() {
        ChapterTest test = new ChapterTest();
        test.setId(30);
        test.setStatus(Status.inactive);
        when(chapterTestRepository.findById(30)).thenReturn(Optional.of(test));

        assertThatThrownBy(() -> chapterTestService.getDetails("jwt", new ChapterTestDetailsRequest(30)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Test not found");
    }
}
