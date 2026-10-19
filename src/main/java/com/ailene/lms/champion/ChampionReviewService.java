package com.ailene.lms.champion;

import com.ailene.lms.common.CategorySummary;
import com.ailene.lms.common.exception.BadRequestException;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import com.ailene.lms.notification.NotificationWriter;
import com.ailene.lms.prompt.Prompt;
import com.ailene.lms.prompt.PromptEvaluation;
import com.ailene.lms.prompt.PromptRepository;
import com.ailene.lms.prompt.PromptSubmission;
import com.ailene.lms.prompt.PromptSubmissionRepository;
import com.ailene.lms.usecase.UseCase;
import com.ailene.lms.usecase.UseCaseRepository;
import com.ailene.lms.usecase.UseCaseSubmission;
import com.ailene.lms.usecase.UseCaseSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChampionReviewService {

    private static final String PROMPT_LEARNING_TYPE = "prompt";
    private static final String USE_CASE_LEARNING_TYPE = "use_case";

    private final ChampionAccessGuard championAccessGuard;
    private final ChampionRepository championRepository;
    private final PromptRepository promptRepository;
    private final UseCaseRepository useCaseRepository;
    private final PromptSubmissionRepository promptSubmissionRepository;
    private final UseCaseSubmissionRepository useCaseSubmissionRepository;
    private final NotificationWriter notificationWriter;

    public ReviewQueueResponse getPromptQueue(String jwt, ChampionProjectRequest request) {
        ChampionContext champion = championAccessGuard.requireChampion(jwt, request.projectId());
        List<ReviewQueueProjection> rows = championRepository.findPromptReviewQueue(champion.projectId(), champion.groupId());
        Map<Integer, List<CategorySummary>> categories = promptCategories(rows);

        return new ReviewQueueResponse(rows.stream().map(row -> toQueueItem(row, categories, true)).toList());
    }

    public ReviewQueueResponse getUseCaseQueue(String jwt, ChampionProjectRequest request) {
        ChampionContext champion = championAccessGuard.requireChampion(jwt, request.projectId());
        List<ReviewQueueProjection> rows = championRepository.findUseCaseReviewQueue(champion.projectId(), champion.groupId());
        Map<Integer, List<CategorySummary>> categories = useCaseCategories(rows);

        return new ReviewQueueResponse(rows.stream().map(row -> toQueueItem(row, categories, false)).toList());
    }

    public PromptSubmissionDetail getPromptDetail(String jwt, SubmissionDetailRequest request) {
        ChampionContext champion = championAccessGuard.requireChampion(jwt, request.projectId());

        PromptSubmissionDetailProjection row = championRepository
                .findPromptSubmissionDetail(request.submissionId(), champion.projectId(), champion.groupId()).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));

        return new PromptSubmissionDetail(row.getId(),
                new PromptSubject(row.getItemId(), row.getItemName(), row.getItemText(), row.getExpectedOutput(),
                        new TeamLevelRef(row.getLevelId(), row.getLevelNumber(), row.getLevelName())),
                new SubmissionMember(row.getAccessId(), row.getFullName(), row.getEmail(), row.getAvatar()),
                row.getReviewerAccessId() == null ? null
                        : new SubmissionReviewer(row.getReviewerAccessId(), row.getReviewerName(),
                                row.getReviewerAvatar()),
                row.getDeadline(), row.getMessage(), row.getInput(), row.getOutput(), row.getSubmittedAt(),
                row.getReviewedAt(), row.getComment(), Boolean.TRUE.equals(row.getAccepted()),
                row.getSubmittedAt() == null ? null : PromptEvaluation.from(row),
                categoriesFor(championRepository.findPromptCategories(List.of(row.getItemId())), row.getItemId()));
    }

    public UseCaseSubmissionDetail getUseCaseDetail(String jwt, SubmissionDetailRequest request) {
        ChampionContext champion = championAccessGuard.requireChampion(jwt, request.projectId());

        UseCaseSubmissionDetailProjection row = championRepository
                .findUseCaseSubmissionDetail(request.submissionId(), champion.projectId(), champion.groupId()).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));

        return new UseCaseSubmissionDetail(row.getId(),
                new UseCaseSubject(row.getItemId(), row.getItemName(), row.getItemText(),
                        new TeamLevelRef(row.getLevelId(), row.getLevelNumber(), row.getLevelName())),
                new SubmissionMember(row.getAccessId(), row.getFullName(), row.getEmail(), row.getAvatar()),
                row.getReviewerAccessId() == null ? null
                        : new SubmissionReviewer(row.getReviewerAccessId(), row.getReviewerName(),
                                row.getReviewerAvatar()),
                row.getDeadline(), row.getMessage(), row.getOutcomeProof(), row.getHoursWithAi(),
                row.getHoursWithoutAi(), row.getDescription(), row.getAiTool(), row.getFrequency(), row.getType(),
                row.getSubmittedAt(), row.getReviewedAt(), row.getComment(), Boolean.TRUE.equals(row.getAccepted()),
                categoriesFor(championRepository.findUseCaseCategories(List.of(row.getItemId())), row.getItemId()));
    }

    @Transactional
    public ReviewResult reviewPrompt(String jwt, ReviewPromptRequest request) {
        ChampionContext champion = championAccessGuard.requireChampion(jwt, request.projectId());

        PromptSubmission submission = promptSubmissionRepository.findById(request.submissionId())
                .filter(row -> inChampionGroup(champion, row.getStudentAccessId()))
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));
        requireReviewable(submission.getSubmittedAt(), submission.getIsAccepted(), request.isAccepted(),
                request.comment());

        submission.setReviewedByAccessId(champion.accessId());
        submission.setReviewedAt(OffsetDateTime.now());
        submission.setComment(trimToNull(request.comment()));
        submission.setIsAccepted(request.isAccepted());
        // Only the dimensions the champion sent replace the AI's score; the rest keep it.
        if (request.rubricSpecificity() != null) {
            submission.setRubricSpecificity(request.rubricSpecificity());
        }
        if (request.rubricContext() != null) {
            submission.setRubricContext(request.rubricContext());
        }
        if (request.rubricConstraints() != null) {
            submission.setRubricConstraints(request.rubricConstraints());
        }
        if (request.rubricExamples() != null) {
            submission.setRubricExamples(request.rubricExamples());
        }
        if (request.rubricIteration() != null) {
            submission.setRubricIteration(request.rubricIteration());
        }
        promptSubmissionRepository.save(submission);

        Prompt prompt = promptRepository.findById(submission.getPromptId())
                .orElseThrow(() -> new ResourceNotFoundException("Prompt not found"));
        boolean accepted = Boolean.TRUE.equals(request.isAccepted());
        short xpAwarded = 0;
        if (accepted) {
            xpAwarded = prompt.getXpReward() == null ? 0 : prompt.getXpReward();
            championRepository.insertXpEarning(submission.getStudentAccessId(), PROMPT_LEARNING_TYPE,
                    String.valueOf(submission.getPromptId()), xpAwarded);
        }
        notificationWriter.promptReviewed(submission.getStudentAccessId(), prompt.getId(), prompt.getName(),
                accepted);

        return new ReviewResult(xpAwarded);
    }

    @Transactional
    public ReviewResult reviewUseCase(String jwt, ReviewUseCaseRequest request) {
        ChampionContext champion = championAccessGuard.requireChampion(jwt, request.projectId());

        UseCaseSubmission submission = useCaseSubmissionRepository.findById(request.submissionId())
                .filter(row -> inChampionGroup(champion, row.getStudentAccessId()))
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));
        requireReviewable(submission.getSubmittedAt(), submission.getIsAccepted(), request.isAccepted(),
                request.comment());

        submission.setReviewedByAccessId(champion.accessId());
        submission.setReviewedAt(OffsetDateTime.now());
        submission.setComment(trimToNull(request.comment()));
        submission.setIsAccepted(request.isAccepted());
        useCaseSubmissionRepository.save(submission);

        UseCase useCase = useCaseRepository.findById(submission.getUseCaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Use case not found"));
        boolean accepted = Boolean.TRUE.equals(request.isAccepted());
        short xpAwarded = 0;
        if (accepted) {
            xpAwarded = useCase.getXpReward() == null ? 0 : useCase.getXpReward();
            championRepository.insertXpEarning(submission.getStudentAccessId(), USE_CASE_LEARNING_TYPE,
                    String.valueOf(submission.getUseCaseId()), xpAwarded);
        }
        notificationWriter.useCaseReviewed(submission.getStudentAccessId(), useCase.getId(), useCase.getName(),
                accepted);

        return new ReviewResult(xpAwarded);
    }

    // Any champion of the student's group may review, not only the one the submission was routed to.
    private boolean inChampionGroup(ChampionContext champion, String studentAccessId) {
        return championRepository.isInGroup(studentAccessId, champion.projectId(), champion.groupId());
    }

    private void requireReviewable(OffsetDateTime submittedAt, Boolean alreadyAccepted, Boolean isAccepted,
            String comment) {
        if (submittedAt == null) {
            throw new BadRequestException("Student has not submitted yet");
        }
        if (Boolean.TRUE.equals(alreadyAccepted)) {
            throw new BadRequestException("Submission already accepted");
        }
        if (!Boolean.TRUE.equals(isAccepted) && trimToNull(comment) == null) {
            throw new BadRequestException("Comment required when sending back for revision");
        }
    }

    private ReviewQueueItem toQueueItem(ReviewQueueProjection row, Map<Integer, List<CategorySummary>> categories,
            boolean evaluation) {
        return new ReviewQueueItem(row.getId(),
                new ReviewQueueSubject(row.getItemId(), row.getItemName(), row.getItemText(),
                        new TeamLevelRef(row.getLevelId(), row.getLevelNumber(), row.getLevelName())),
                new ReviewQueueMember(row.getAccessId(), row.getFullName(), row.getAvatar()), row.getDeadline(),
                row.getSubmittedAt(), row.getReviewedAt(), Boolean.TRUE.equals(row.getAccepted()),
                row.getHoursWithAi(), row.getAiTool(),
                evaluation && row.getSubmittedAt() != null ? PromptEvaluation.from(row) : null,
                categories.getOrDefault(row.getItemId(), List.of()));
    }

    private Map<Integer, List<CategorySummary>> promptCategories(List<ReviewQueueProjection> rows) {
        List<Integer> ids = rows.stream().map(ReviewQueueProjection::getItemId).distinct().toList();
        return ids.isEmpty() ? Map.of() : groupCategories(championRepository.findPromptCategories(ids));
    }

    private Map<Integer, List<CategorySummary>> useCaseCategories(List<ReviewQueueProjection> rows) {
        List<Integer> ids = rows.stream().map(ReviewQueueProjection::getItemId).distinct().toList();
        return ids.isEmpty() ? Map.of() : groupCategories(championRepository.findUseCaseCategories(ids));
    }

    private Map<Integer, List<CategorySummary>> groupCategories(List<ItemCategoryProjection> rows) {
        Map<Integer, List<CategorySummary>> grouped = new HashMap<>();
        for (ItemCategoryProjection row : rows) {
            grouped.computeIfAbsent(row.getOwnerId(), key -> new ArrayList<>())
                    .add(new CategorySummary(row.getCategoryId(), row.getCategoryName()));
        }
        return grouped;
    }

    private List<CategorySummary> categoriesFor(List<ItemCategoryProjection> rows, Integer ownerId) {
        return groupCategories(rows).getOrDefault(ownerId, List.of());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
