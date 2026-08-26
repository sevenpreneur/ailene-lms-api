package com.ailene.lms.quiz;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.access.StudentStatusProjection;
import com.ailene.lms.auth.AuthService;
import com.ailene.lms.chapter.Chapter;
import com.ailene.lms.chapter.ChapterRepository;
import com.ailene.lms.common.exception.BadRequestException;
import com.ailene.lms.common.exception.ForbiddenException;
import com.ailene.lms.common.qstash.QStashClient;
import com.ailene.lms.level.Level;
import com.ailene.lms.level.LevelRepository;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

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
    private QuizSubmissionRepository quizSubmissionRepository;
    @Mock
    private QStashClient qStashClient;

    @InjectMocks
    private QuizService quizService;

    private Access mockAccessChain(UUID userId, String quizId, Integer chapterId, Integer levelId,
            short levelNumber, Short callerLevelNumber) {
        Quiz quiz = new Quiz();
        quiz.setId(quizId);
        quiz.setChapterId(chapterId);
        when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));

        Chapter chapter = new Chapter();
        chapter.setId(chapterId);
        chapter.setLevelId(levelId);
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));

        Level level = new Level();
        level.setId(levelId);
        level.setProjectId("proj-1");
        level.setLevelNumber(levelNumber);
        when(levelRepository.findById(levelId)).thenReturn(Optional.of(level));

        Access access = new Access();
        access.setId("access-1");
        when(accessRepository.findByUserIdAndProjectId(userId, "proj-1")).thenReturn(Optional.of(access));

        // saveDraft() never calls requireLevelUnlocked(), so this stub is unused by that test - lenient to allow both.
        if (callerLevelNumber == null) {
            lenient().when(accessRepository.findStudentStatus(userId, "proj-1")).thenReturn(Optional.empty());
        } else {
            StudentStatusProjection status = mock(StudentStatusProjection.class);
            when(status.getCurrentLevelNumber()).thenReturn(callerLevelNumber);
            lenient().when(accessRepository.findStudentStatus(userId, "proj-1")).thenReturn(Optional.of(status));
        }
        return access;
    }

    @Test
    void quizDetails_levelNotUnlocked_throwsForbidden() {
        UUID userId = UUID.randomUUID();
        when(authService.resolveUserId("jwt")).thenReturn(userId);

        mockAccessChain(userId, "quiz-1", 1, 2, (short) 2, (short) 1);

        assertThatThrownBy(() -> quizService.getQuizDetails("jwt", new QuizDetailsRequest("quiz-1")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void startAttempt_noExistingDraft_createsNewSubmissionAndSchedulesAutoSubmit() {
        UUID userId = UUID.randomUUID();
        when(authService.resolveUserId("jwt")).thenReturn(userId);

        mockAccessChain(userId, "quiz-1", 1, 1, (short) 0, null);

        when(quizSubmissionRepository.findByStudentAccessIdAndQuizIdAndCompletedFalse("access-1", "quiz-1"))
                .thenReturn(Optional.empty());
        when(quizSubmissionRepository.findMaxAttemptNumber("access-1", "quiz-1")).thenReturn(null);
        when(quizSubmissionRepository.save(org.mockito.ArgumentMatchers.any(QuizSubmission.class)))
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
    }

    @Test
    void saveDraft_noActiveAttempt_throwsBadRequest() {
        UUID userId = UUID.randomUUID();
        when(authService.resolveUserId("jwt")).thenReturn(userId);

        mockAccessChain(userId, "quiz-1", 1, 1, (short) 0, null);

        when(quizSubmissionRepository.findByStudentAccessIdAndQuizIdAndCompletedFalse("access-1", "quiz-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> quizService.saveDraft("jwt", new QuizUpdateRequest("quiz-1", Map.of())))
                .isInstanceOf(BadRequestException.class);
    }
}
