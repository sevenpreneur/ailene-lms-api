package com.ailene.lms.quiz;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.auth.AuthService;
import com.ailene.lms.chapter.Chapter;
import com.ailene.lms.chapter.ChapterAccessGuard;
import com.ailene.lms.chapter.ChapterLockedReason;
import com.ailene.lms.chapter.ChapterRepository;
import com.ailene.lms.common.exception.BadRequestException;
import com.ailene.lms.common.exception.ForbiddenException;
import com.ailene.lms.common.qstash.QStashClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private AuthService authService;
    @Mock
    private AccessRepository accessRepository;
    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private QuizSubmissionRepository quizSubmissionRepository;
    @Mock
    private QStashClient qStashClient;
    @Mock
    private ChapterAccessGuard chapterAccessGuard;

    @InjectMocks
    private QuizService quizService;

    @BeforeEach
    void setUp() {
        when(authService.resolveUserId("jwt")).thenReturn(USER_ID);

        Quiz quiz = new Quiz();
        quiz.setId("quiz-1");
        quiz.setChapterId(1);
        when(quizRepository.findById("quiz-1")).thenReturn(Optional.of(quiz));

        Chapter chapter = new Chapter();
        chapter.setId(1);
        chapter.setLevelId(1);
        chapter.setProjectId("proj-1");
        when(chapterRepository.findById(1)).thenReturn(Optional.of(chapter));

        Access access = new Access();
        access.setId("access-1");
        access.setProjectId("proj-1");
        when(accessRepository.findByUserIdAndProjectId(USER_ID, "proj-1")).thenReturn(Optional.of(access));
    }

    private void givenChapterLocked() {
        when(chapterAccessGuard.requireUnlocked(1, "access-1"))
                .thenThrow(new ForbiddenException(ChapterAccessGuard.message(ChapterLockedReason.admin_locked)));
    }

    @Test
    void quizDetails_lockedChapter_throwsForbidden() {
        givenChapterLocked();

        assertThatThrownBy(() -> quizService.getQuizDetails("jwt", new QuizDetailsRequest("quiz-1")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void startAttempt_lockedChapter_createsNothing() {
        givenChapterLocked();

        assertThatThrownBy(() -> quizService.startAttempt("jwt", new QuizAttemptRequest("quiz-1")))
                .isInstanceOf(ForbiddenException.class);
        verify(quizSubmissionRepository, never()).save(any());
        verifyNoInteractions(qStashClient);
    }

    @Test
    void saveDraft_lockedChapter_throwsForbidden() {
        givenChapterLocked();

        assertThatThrownBy(() -> quizService.saveDraft("jwt", new QuizUpdateRequest("quiz-1", Map.of())))
                .isInstanceOf(ForbiddenException.class);
        verify(quizSubmissionRepository, never()).save(any());
    }

    @Test
    void submit_lockedChapter_throwsForbidden() {
        givenChapterLocked();

        assertThatThrownBy(() -> quizService.submit("jwt", new QuizSubmitRequest("quiz-1", Map.of())))
                .isInstanceOf(ForbiddenException.class);
        verify(quizSubmissionRepository, never()).save(any());
    }

    @Test
    void result_lockedChapter_throwsForbidden() {
        givenChapterLocked();

        assertThatThrownBy(() -> quizService.getResult("jwt", new QuizResultRequest("quiz-1")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void startAttempt_noExistingDraft_createsNewSubmissionAndSchedulesAutoSubmit() {
        when(quizSubmissionRepository.findByStudentAccessIdAndQuizIdAndCompletedFalse("access-1", "quiz-1"))
                .thenReturn(Optional.empty());
        when(quizSubmissionRepository.findMaxAttemptNumber("access-1", "quiz-1")).thenReturn(null);
        when(quizSubmissionRepository.save(any(QuizSubmission.class)))
                .thenAnswer(invocation -> {
                    QuizSubmission submission = invocation.getArgument(0);
                    submission.setId(42);
                    return submission;
                });

        QuizAttemptResponse response = quizService.startAttempt("jwt", new QuizAttemptRequest("quiz-1"));

        assertThat(response.status()).isEqualTo("active");
        assertThat(response.submissionId()).isEqualTo(42);
        assertThat(response.secondsLeft()).isEqualTo(1200);
        assertThat(response.answers()).isEmpty();
        verify(chapterAccessGuard).requireUnlocked(1, "access-1");
    }

    @Test
    void saveDraft_noActiveAttempt_throwsBadRequest() {
        when(quizSubmissionRepository.findByStudentAccessIdAndQuizIdAndCompletedFalse("access-1", "quiz-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> quizService.saveDraft("jwt", new QuizUpdateRequest("quiz-1", Map.of())))
                .isInstanceOf(BadRequestException.class);
    }
}
