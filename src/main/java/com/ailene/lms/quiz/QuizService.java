package com.ailene.lms.quiz;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.access.StudentStatusProjection;
import com.ailene.lms.auth.AuthService;
import com.ailene.lms.chapter.Chapter;
import com.ailene.lms.chapter.ChapterRepository;
import com.ailene.lms.common.ChapterSummary;
import com.ailene.lms.common.exception.BadRequestException;
import com.ailene.lms.common.exception.ForbiddenException;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import com.ailene.lms.common.qstash.QStashClient;
import com.ailene.lms.level.Level;
import com.ailene.lms.level.LevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuizService {

    private static final int QUIZ_DURATION_SECONDS = 20 * 60;

    private final AuthService authService;
    private final AccessRepository accessRepository;
    private final ChapterRepository chapterRepository;
    private final LevelRepository levelRepository;
    private final QuizRepository quizRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;
    private final QStashClient qStashClient;

    public QuizDetailsResponse getQuizDetails(String jwt, QuizDetailsRequest request) {
        UUID userId = authService.resolveUserId(jwt);
        Quiz quiz = resolveQuiz(request.quizId());
        AccessContext ctx = resolveAccessAndRequireUnlock(userId, quiz);
        Access access = ctx.access();

        QuizStatsProjection stats = quizRepository.findQuizStats(quiz.getId(), access.getId());

        Map<Integer, QuizQuestionOptionProjection> firstRowByQuestion = new LinkedHashMap<>();
        Map<Integer, List<QuizOptionItem>> optionsByQuestion = new LinkedHashMap<>();
        for (QuizQuestionOptionProjection row : quizRepository.findQuestionsWithOptions(quiz.getId())) {
            firstRowByQuestion.putIfAbsent(row.getQuestionId(), row);
            optionsByQuestion.computeIfAbsent(row.getQuestionId(), id -> new ArrayList<>())
                    .add(new QuizOptionItem(row.getOptionId(), row.getOptionCode(), row.getOptionText()));
        }
        List<QuizQuestionItem> questions = firstRowByQuestion.values().stream()
                .map(row -> new QuizQuestionItem(row.getQuestionId(), row.getQuestion(), row.getQuestionOrderIndex(),
                        row.getQuestionXpReward(), optionsByQuestion.get(row.getQuestionId())))
                .toList();

        return new QuizDetailsResponse(quiz.getId(), quiz.getName(), quiz.getDescription(), quiz.getOrderIndex(),
                new ChapterSummary(ctx.chapter().getId(), ctx.chapter().getName()), stats.getQuestionCount(),
                stats.getXpReward(), stats.getAttempts(), questions);
    }

    @Transactional
    public QuizAttemptResponse startAttempt(String jwt, QuizAttemptRequest request) {
        UUID userId = authService.resolveUserId(jwt);
        Quiz quiz = resolveQuiz(request.quizId());
        Access access = resolveAccessAndRequireUnlock(userId, quiz).access();

        QuizSubmission existing = quizSubmissionRepository
                .findByStudentAccessIdAndQuizIdAndCompletedFalse(access.getId(), quiz.getId())
                .orElse(null);

        if (existing != null) {
            int secondsLeft = secondsLeft(existing.getStartedAt());
            if (secondsLeft <= 0) {
                finalizeSubmission(existing, quiz.getId(), null);
                return QuizAttemptResponse.finalized();
            }
            return QuizAttemptResponse.active(existing.getId(), existing.getStartedAt(), OffsetDateTime.now(),
                    secondsLeft, existing.getAnswers());
        }

        QuizSubmission created = new QuizSubmission();
        created.setStudentAccessId(access.getId());
        created.setQuizId(quiz.getId());
        created.setAttemptNumber(nextAttemptNumber(access.getId(), quiz.getId()));
        created.setAnswers(new LinkedHashMap<>());
        created.setScore((short) 0);
        created.setCompleted(false);
        OffsetDateTime now = OffsetDateTime.now();
        created.setStartedAt(now);
        created.setSubmittedAt(now);
        created = quizSubmissionRepository.save(created);

        qStashClient.publishDelayed("/api/v1/quizzes/auto-submit", new QuizAutoSubmitRequest(created.getId()),
                QUIZ_DURATION_SECONDS);

        return QuizAttemptResponse.active(created.getId(), now, now, QUIZ_DURATION_SECONDS, created.getAnswers());
    }

    @Transactional
    public QuizUpdateResponse saveDraft(String jwt, QuizUpdateRequest request) {
        UUID userId = authService.resolveUserId(jwt);
        Quiz quiz = resolveQuiz(request.quizId());
        Access access = resolveAccess(userId, quiz).access();

        QuizSubmission draft = quizSubmissionRepository
                .findByStudentAccessIdAndQuizIdAndCompletedFalse(access.getId(), quiz.getId())
                .orElseThrow(() -> new BadRequestException("No active attempt. Start an attempt first."));

        int secondsLeft = secondsLeft(draft.getStartedAt());
        if (secondsLeft <= 0) {
            finalizeSubmission(draft, quiz.getId(), null);
            return new QuizUpdateResponse("finalized");
        }

        draft.setAnswers(request.answers());
        quizSubmissionRepository.save(draft);

        return new QuizUpdateResponse("active");
    }

    @Transactional
    public QuizSubmitResponse submit(String jwt, QuizSubmitRequest request) {
        UUID userId = authService.resolveUserId(jwt);
        Quiz quiz = resolveQuiz(request.quizId());
        Access access = resolveAccessAndRequireUnlock(userId, quiz).access();

        QuizSubmission submission = quizSubmissionRepository
                .findByStudentAccessIdAndQuizIdAndCompletedFalse(access.getId(), quiz.getId())
                .orElse(null);

        short attemptNumber;
        if (submission != null) {
            attemptNumber = submission.getAttemptNumber();
        } else {
            attemptNumber = nextAttemptNumber(access.getId(), quiz.getId());
            submission = new QuizSubmission();
            submission.setStudentAccessId(access.getId());
            submission.setQuizId(quiz.getId());
            submission.setAttemptNumber(attemptNumber);
            submission.setAnswers(new LinkedHashMap<>());
            submission.setScore((short) 0);
            submission.setCompleted(false);
            OffsetDateTime now = OffsetDateTime.now();
            submission.setStartedAt(now);
            submission.setSubmittedAt(now);
            submission = quizSubmissionRepository.save(submission);
        }

        FinalizeResult result = finalizeSubmission(submission, quiz.getId(), request.answers());

        return new QuizSubmitResponse(result.score(), result.xpAwarded(), attemptNumber);
    }

    public QuizResultResponse getResult(String jwt, QuizResultRequest request) {
        UUID userId = authService.resolveUserId(jwt);
        Quiz quiz = resolveQuiz(request.quizId());
        AccessContext ctx = resolveAccess(userId, quiz);
        Access access = ctx.access();

        QuizSubmission latestCompleted = quizSubmissionRepository
                .findFirstByStudentAccessIdAndQuizIdAndCompletedTrueOrderByAttemptNumberDesc(access.getId(),
                        quiz.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz submission not found"));

        Short xpEarned = quizRepository.findXpEarned(quiz.getId(), access.getId());

        Map<Integer, QuizResultQuestionOptionProjection> firstRowByQuestion = new LinkedHashMap<>();
        Map<Integer, List<QuizResultOptionItem>> optionsByQuestion = new LinkedHashMap<>();
        for (QuizResultQuestionOptionProjection row : quizRepository.findQuestionsWithAnswerKey(quiz.getId())) {
            firstRowByQuestion.putIfAbsent(row.getQuestionId(), row);
            optionsByQuestion.computeIfAbsent(row.getQuestionId(), id -> new ArrayList<>())
                    .add(new QuizResultOptionItem(row.getOptionId(), row.getOptionCode(), row.getOptionText(),
                            row.getOptionIsCorrect()));
        }
        List<QuizResultQuestionItem> questions = firstRowByQuestion.values().stream()
                .map(row -> new QuizResultQuestionItem(row.getQuestionId(), row.getQuestion(),
                        row.getQuestionOrderIndex(), row.getQuestionXpReward(), row.getExplanation(),
                        optionsByQuestion.get(row.getQuestionId())))
                .toList();

        QuizResultSubmission submission = new QuizResultSubmission(latestCompleted.getAttemptNumber(),
                latestCompleted.getScore(), latestCompleted.getAnswers(), latestCompleted.getSubmittedAt());

        return new QuizResultResponse(quiz.getId(), quiz.getName(), quiz.getDescription(),
                new ChapterSummary(ctx.chapter().getId(), ctx.chapter().getName()), questions, submission,
                xpEarned == null ? 0 : xpEarned);
    }

    @Transactional
    public void autoSubmit(Integer submissionId) {
        QuizSubmission submission = quizSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz submission not found"));
        if (Boolean.TRUE.equals(submission.getCompleted())) {
            return;
        }
        finalizeSubmission(submission, submission.getQuizId(), null);
    }

    private FinalizeResult finalizeSubmission(QuizSubmission submission, String quizId,
            Map<String, String> overrideAnswers) {
        if (Boolean.TRUE.equals(submission.getCompleted())) {
            return new FinalizeResult(submission.getScore(), (short) 0, true);
        }

        Map<String, String> answers = overrideAnswers != null ? overrideAnswers
                : submission.getAnswers() != null ? submission.getAnswers() : Map.of();

        List<QuizAnswerKeyProjection> answerKey = quizRepository.findAnswerKey(quizId);
        int correct = 0;
        int xpFromCorrect = 0;
        for (QuizAnswerKeyProjection q : answerKey) {
            String selected = answers.get(String.valueOf(q.getQuestionId()));
            if (q.getCorrectOptionCode() != null && q.getCorrectOptionCode().equals(selected)) {
                correct++;
                xpFromCorrect += q.getXpReward();
            }
        }
        short score = answerKey.isEmpty() ? 0 : (short) Math.round((correct * 100.0) / answerKey.size());

        submission.setAnswers(answers);
        submission.setScore(score);
        submission.setCompleted(true);
        submission.setSubmittedAt(OffsetDateTime.now());
        quizSubmissionRepository.save(submission);

        Short previousXp = quizRepository.findXpEarned(quizId, submission.getStudentAccessId());
        short previous = previousXp == null ? 0 : previousXp;
        short finalXp = (short) Math.max(previous, xpFromCorrect);
        short xpAwarded = 0;
        if (previousXp == null || finalXp > previous) {
            quizRepository.upsertXpEarning(quizId, submission.getStudentAccessId(), finalXp);
            xpAwarded = (short) (finalXp - previous);
        }

        return new FinalizeResult(score, xpAwarded, false);
    }

    private int secondsLeft(OffsetDateTime startedAt) {
        long elapsed = Duration.between(startedAt, OffsetDateTime.now()).toSeconds();
        return (int) Math.max(0, QUIZ_DURATION_SECONDS - elapsed);
    }

    private short nextAttemptNumber(String accessId, String quizId) {
        Short max = quizSubmissionRepository.findMaxAttemptNumber(accessId, quizId);
        return (short) ((max == null ? 0 : max) + 1);
    }

    private Quiz resolveQuiz(String quizId) {
        return quizRepository.findById(quizId).orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));
    }

    private AccessContext resolveAccess(UUID userId, Quiz quiz) {
        Chapter chapter = chapterRepository.findById(quiz.getChapterId())
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));
        Level level = levelRepository.findById(chapter.getLevelId())
                .orElseThrow(() -> new ResourceNotFoundException("Level not found"));
        Access access = accessRepository.findByUserIdAndProjectId(userId, level.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));
        return new AccessContext(access, chapter, level);
    }

    private AccessContext resolveAccessAndRequireUnlock(UUID userId, Quiz quiz) {
        AccessContext ctx = resolveAccess(userId, quiz);
        requireLevelUnlocked(userId, ctx.level());
        return ctx;
    }

    private void requireLevelUnlocked(UUID userId, Level level) {
        StudentStatusProjection status = accessRepository.findStudentStatus(userId, level.getProjectId())
                .orElse(null);
        short currentLevelNumber = status == null || status.getCurrentLevelNumber() == null ? 0
                : status.getCurrentLevelNumber();
        if (level.getLevelNumber() > currentLevelNumber) {
            throw new ForbiddenException("This level hasn't been unlocked yet.");
        }
    }

    private record AccessContext(Access access, Chapter chapter, Level level) {
    }

    private record FinalizeResult(short score, short xpAwarded, boolean alreadyCompleted) {
    }
}
