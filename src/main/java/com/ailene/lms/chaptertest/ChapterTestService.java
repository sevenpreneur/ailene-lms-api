package com.ailene.lms.chaptertest;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.auth.AuthService;
import com.ailene.lms.chapter.Chapter;
import com.ailene.lms.chapter.ChapterAccessGuard;
import com.ailene.lms.chapter.ChapterRepository;
import com.ailene.lms.common.ChapterSummary;
import com.ailene.lms.common.Status;
import com.ailene.lms.common.exception.ConflictException;
import com.ailene.lms.common.exception.ForbiddenException;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChapterTestService {

    private final AuthService authService;
    private final AccessRepository accessRepository;
    private final ChapterRepository chapterRepository;
    private final ChapterTestRepository chapterTestRepository;
    private final ChapterTestSubmissionRepository chapterTestSubmissionRepository;
    private final ChapterAccessGuard chapterAccessGuard;

    public ChapterTestDetailsResponse getDetails(String jwt, ChapterTestDetailsRequest request) {
        UUID userId = authService.resolveUserId(jwt);
        ChapterTest test = resolveTest(request.testId());
        Chapter chapter = resolveChapter(test.getChapterId());
        Access access = resolveUnlocked(userId, chapter);

        ChapterTestPair pair = ChapterTestPair.from(
                chapterTestRepository.findChapterTests(chapter.getId(), access.getId()));
        ChapterTestLockedReason lockedReason = lockedReason(pair, test.getKind(), chapter.getId(), access.getId());

        List<ChapterTestQuestion> questions = ChapterTestQuestion.group(
                chapterTestRepository.findQuestionsWithOptions(test.getId()));
        int choiceCount = (int) questions.stream().filter(ChapterTestQuestion::isMultipleChoice).count();

        return new ChapterTestDetailsResponse(test.getId(), test.getKind(), test.getName(), test.getDescription(),
                new ChapterSummary(chapter.getId(), chapter.getName()), questions.size(), choiceCount,
                questions.size() - choiceCount, ChapterTestPair.submitted(pair.get(test.getKind())),
                lockedReason == null, lockedReason,
                questions.stream().map(ChapterTestQuestionItem::from).toList());
    }

    @Transactional
    public ChapterTestSubmitResponse submit(String jwt, ChapterTestSubmitRequest request) {
        UUID userId = authService.resolveUserId(jwt);
        ChapterTest test = resolveTest(request.testId());
        Chapter chapter = resolveChapter(test.getChapterId());
        Access access = resolveUnlocked(userId, chapter);

        ChapterTestPair pair = ChapterTestPair.from(
                chapterTestRepository.findChapterTests(chapter.getId(), access.getId()));
        if (ChapterTestPair.submitted(pair.get(test.getKind()))) {
            throw new ConflictException("You already submitted this test.");
        }
        ChapterTestLockedReason lockedReason = lockedReason(pair, test.getKind(), chapter.getId(), access.getId());
        if (lockedReason != null) {
            throw new ForbiddenException(lockedMessage(lockedReason));
        }

        List<ChapterTestQuestion> questions = ChapterTestQuestion.group(
                chapterTestRepository.findQuestionsWithOptions(test.getId()));
        ChapterTestScorer.Result result = ChapterTestScorer.score(questions, request.answers());

        ChapterTestSubmission submission = new ChapterTestSubmission();
        submission.setStudentAccessId(access.getId());
        submission.setTestId(test.getId());
        submission.setAnswers(result.answers());
        submission.setScore(result.score());
        submission.setCorrectCount(result.correctCount());
        submission.setChoiceCount(result.choiceCount());
        submission.setSubmittedAt(OffsetDateTime.now());
        try {
            submission = chapterTestSubmissionRepository.saveAndFlush(submission);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("You already submitted this test.");
        }

        return new ChapterTestSubmitResponse(result.score(), result.correctCount(), result.choiceCount(),
                result.essayCount(), submission.getSubmittedAt());
    }

    public ChapterTestResultResponse getResult(String jwt, ChapterTestResultRequest request) {
        UUID userId = authService.resolveUserId(jwt);
        ChapterTest test = resolveTest(request.testId());
        Chapter chapter = resolveChapter(test.getChapterId());
        Access access = resolveUnlocked(userId, chapter);

        ChapterTestSubmission submission = chapterTestSubmissionRepository
                .findByStudentAccessIdAndTestId(access.getId(), test.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Test submission not found"));

        ChapterTestPair pair = ChapterTestPair.from(
                chapterTestRepository.findChapterTests(chapter.getId(), access.getId()));
        boolean reveal = pair.revealsAnswers();
        Map<String, String> answers = submission.getAnswers() == null ? Map.of() : submission.getAnswers();

        List<ChapterTestResultQuestionItem> questions = ChapterTestQuestion
                .group(chapterTestRepository.findQuestionsWithOptions(test.getId())).stream()
                .map(q -> ChapterTestResultQuestionItem.from(q, answers.get(String.valueOf(q.id())), reveal))
                .toList();

        Short preTestScore = test.getKind() == ChapterTestKind.post_test && pair.preTest() != null
                ? pair.preTest().getScore()
                : null;

        return new ChapterTestResultResponse(test.getId(), test.getKind(), test.getName(), test.getDescription(),
                new ChapterSummary(chapter.getId(), chapter.getName()), reveal,
                ChapterTestResultSubmission.from(submission), questions, preTestScore);
    }

    public ChapterTestTasks findTasks(Integer chapterId, String accessId) {
        ChapterTestPair pair = ChapterTestPair.from(chapterTestRepository.findChapterTests(chapterId, accessId));
        ChapterTestTaskItem preTest = pair.preTest() == null ? null
                : ChapterTestTaskItem.from(pair.preTest(),
                        lockedReason(pair, ChapterTestKind.pre_test, chapterId, accessId));
        ChapterTestTaskItem postTest = pair.postTest() == null ? null
                : ChapterTestTaskItem.from(pair.postTest(),
                        lockedReason(pair, ChapterTestKind.post_test, chapterId, accessId));
        return new ChapterTestTasks(preTest, postTest);
    }

    private ChapterTestLockedReason lockedReason(ChapterTestPair pair, ChapterTestKind kind, Integer chapterId,
            String accessId) {
        long incompleteMaterials = kind == ChapterTestKind.post_test && !ChapterTestPair.submitted(pair.get(kind))
                ? chapterTestRepository.countIncompleteMaterials(chapterId, accessId)
                : 0;
        return pair.lockedReason(kind, incompleteMaterials);
    }

    private static String lockedMessage(ChapterTestLockedReason reason) {
        return switch (reason) {
            case pre_test_required -> "Finish this chapter's pre-test first.";
            case materials_incomplete -> "Read every material in this chapter before the post-test.";
            case post_test_submitted -> "The pre-test closes once the post-test is submitted.";
        };
    }

    private ChapterTest resolveTest(Integer testId) {
        return chapterTestRepository.findById(testId)
                .filter(t -> t.getStatus() == Status.active)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found"));
    }

    private Chapter resolveChapter(Integer chapterId) {
        return chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));
    }

    private Access resolveUnlocked(UUID userId, Chapter chapter) {
        Access access = accessRepository.findByUserIdAndProjectId(userId, chapter.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));
        chapterAccessGuard.requireUnlocked(chapter.getId(), access.getId());
        return access;
    }
}
