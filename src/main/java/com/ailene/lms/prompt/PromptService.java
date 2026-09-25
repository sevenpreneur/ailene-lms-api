package com.ailene.lms.prompt;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.access.GroupSummaryProjection;
import com.ailene.lms.common.AssignedByUser;
import com.ailene.lms.common.CategorySummary;
import com.ailene.lms.common.Status;
import com.ailene.lms.common.TimeUtils;
import com.ailene.lms.common.exception.BadRequestException;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import com.ailene.lms.common.pagination.PageMeta;
import com.ailene.lms.common.pagination.PagedResponse;
import com.ailene.lms.common.pagination.Pagination;
import com.ailene.lms.level.Level;
import com.ailene.lms.level.LevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromptService {

    private static final short SELF_CREATE_LEVEL_NUMBER = 2;
    private static final String SELF_PRACTICE_PLACEHOLDER = "(Latihan mandiri — tanpa target output)";

    private final PromptRepository promptRepository;
    private final PromptSubmissionRepository promptSubmissionRepository;
    private final AccessRepository accessRepository;
    private final LevelRepository levelRepository;
    private final PromptEvaluationService promptEvaluationService;

    public PagedResponse<PromptListItem> list(UUID userId, PromptListRequest request) {
        int page = Pagination.normalizePage(request.page());
        int pageSize = Pagination.normalizePageSize(request.pageSize());
        String search = request.search() == null ? "" : request.search();

        long total = promptRepository.countPrompts(request.projectId(), search);
        List<PromptListProjection> prompts = total == 0
                ? List.of()
                : promptRepository.findPromptPage(request.projectId(), search, pageSize,
                        Pagination.offset(page, pageSize));

        List<Integer> promptIds = prompts.stream().map(PromptListProjection::getId).toList();

        Map<Integer, List<CategorySummary>> categoriesByPrompt = promptIds.isEmpty()
                ? Map.of()
                : promptRepository.findCategoriesForPrompts(promptIds).stream()
                        .collect(Collectors.groupingBy(PromptCategoryProjection::getPromptId,
                                Collectors.mapping(c -> new CategorySummary(c.getId(), c.getName()),
                                        Collectors.toList())));

        Map<Integer, PromptSubmissionProjection> submissionByPrompt = new HashMap<>();
        if (!promptIds.isEmpty()) {
            accessRepository.findByUserIdAndProjectId(userId, request.projectId())
                    .ifPresent(access -> promptRepository.findSubmissionsForAccess(access.getId(), promptIds)
                            .forEach(submission -> submissionByPrompt.put(submission.getPromptId(), submission)));
        }

        List<PromptListItem> items = prompts.stream()
                .map(prompt -> toItem(prompt, categoriesByPrompt.get(prompt.getId()),
                        submissionByPrompt.get(prompt.getId())))
                .toList();

        PageMeta meta = Pagination.meta(total, page, pageSize);
        return new PagedResponse<>(items, meta);
    }

    private PromptListItem toItem(PromptListProjection prompt, List<CategorySummary> categories,
            PromptSubmissionProjection submission) {
        return new PromptListItem(prompt.getId(), prompt.getName(), prompt.getDescription(),
                prompt.getLevelNumber(), categories == null ? List.of() : categories,
                TimeUtils.toOffsetDateTime(submission == null ? null : submission.getDeadlineAt()),
                TimeUtils.toOffsetDateTime(submission == null ? null : submission.getSubmittedAt()),
                TimeUtils.toOffsetDateTime(submission == null ? null : submission.getReviewedAt()),
                submission == null ? null : submission.getIsAccepted());
    }

    public List<PromptAssignedItem> listAssigned(UUID userId, PromptAssignedRequest request) {
        Access access = accessRepository.findByUserIdAndProjectId(userId, request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));

        List<PromptAssignedProjection> assigned = promptRepository.findAssignedPrompts(request.projectId(),
                access.getId(), request.hasSubmitted(), request.isAccepted());

        List<Integer> promptIds = assigned.stream().map(PromptAssignedProjection::getId).toList();
        Map<Integer, List<CategorySummary>> categoriesByPrompt = promptIds.isEmpty()
                ? Map.of()
                : promptRepository.findCategoriesForPrompts(promptIds).stream()
                        .collect(Collectors.groupingBy(PromptCategoryProjection::getPromptId,
                                Collectors.mapping(c -> new CategorySummary(c.getId(), c.getName()),
                                        Collectors.toList())));

        return assigned.stream()
                .map(prompt -> toAssignedItem(prompt, categoriesByPrompt.getOrDefault(prompt.getId(), List.of())))
                .toList();
    }

    public PromptDetailsResponse getDetails(UUID userId, PromptDetailsRequest request) {
        Prompt prompt = promptRepository.findById(request.id())
                .orElseThrow(() -> new ResourceNotFoundException("Prompt not found"));
        Level level = levelRepository.findById(prompt.getLevelId())
                .orElseThrow(() -> new ResourceNotFoundException("Level not found"));
        Access access = accessRepository.findByUserIdAndProjectId(userId, prompt.getOnlyProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));

        List<CategorySummary> categories = promptRepository.findCategoriesForPrompts(List.of(prompt.getId())).stream()
                .map(c -> new CategorySummary(c.getId(), c.getName()))
                .toList();

        PromptSubmissionProjection submission = promptRepository
                .findSubmissionsForAccess(access.getId(), List.of(prompt.getId())).stream()
                .findFirst()
                .orElse(null);

        return new PromptDetailsResponse(prompt.getId(), prompt.getName(), prompt.getScenario(),
                prompt.getExpectedOutput(), level.getId(), level.getLevelNumber(), categories, prompt.getXpReward(),
                prompt.getIsSelfCreated(),
                TimeUtils.toOffsetDateTime(submission == null ? null : submission.getDeadlineAt()),
                TimeUtils.toOffsetDateTime(submission == null ? null : submission.getSubmittedAt()),
                TimeUtils.toOffsetDateTime(submission == null ? null : submission.getReviewedAt()),
                submission == null ? null : submission.getIsAccepted(),
                submission == null ? null : submission.getInput(),
                submission == null ? null : submission.getOutput(),
                submission == null ? null : submission.getComment(),
                submission == null || submission.getSubmittedAt() == null ? null : PromptEvaluation.from(submission));
    }

    @Transactional
    public PromptDetailsResponse selfCreate(UUID userId, PromptSelfCreateRequest request) {
        GroupSummaryProjection summary = accessRepository.findGroupSummary(userId, request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));
        if (summary.getGroupId() == null) {
            throw new BadRequestException(
                    "You're not part of a group yet, so self-created practice isn't available.");
        }
        String championAccessId = accessRepository
                .findChampionAccessId(request.projectId(), summary.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("No champion found for this group"));

        List<Short> categoryIds = request.categoryIds().stream().distinct().toList();
        if (promptRepository.countExistingCategories(categoryIds) != categoryIds.size()) {
            throw new ResourceNotFoundException("Some categories were not found");
        }

        Level level = levelRepository.findByLevelNumber(SELF_CREATE_LEVEL_NUMBER)
                .orElseThrow(() -> new ResourceNotFoundException("Prompt level (L2) not found"));

        Prompt prompt = new Prompt();
        prompt.setLevelId(level.getId());
        prompt.setOnlyProjectId(request.projectId());
        prompt.setName(request.name());
        prompt.setScenario(request.scenario());
        prompt.setExpectedOutput(SELF_PRACTICE_PLACEHOLDER);
        prompt.setStatus(Status.active);
        prompt.setIsSelfCreated(true);
        Integer promptId = promptRepository.save(prompt).getId();

        for (Short categoryId : categoryIds) {
            promptRepository.insertCategory(promptId, categoryId);
        }

        PromptSubmission submission = new PromptSubmission();
        submission.setStudentAccessId(summary.getAccessId());
        submission.setPromptId(promptId);
        submission.setAssignedByAccessId(championAccessId);
        submission.setInput(request.input());
        submission.setOutput(request.output());
        submission.setSubmittedAt(OffsetDateTime.now());
        promptEvaluationService.markPending(submission);
        promptSubmissionRepository.save(submission);
        promptEvaluationService.scheduleAfterCommit(submission);

        return getDetails(userId, new PromptDetailsRequest(promptId));
    }

    @Transactional
    public PromptDetailsResponse selfAssign(UUID userId, PromptSelfAssignRequest request) {
        Prompt prompt = promptRepository.findById(request.promptId())
                .filter(p -> p.getStatus() == Status.active)
                .orElseThrow(() -> new ResourceNotFoundException("Prompt not found"));
        Level level = levelRepository.findById(prompt.getLevelId())
                .orElseThrow(() -> new ResourceNotFoundException("Level not found"));

        GroupSummaryProjection summary = accessRepository.findGroupSummary(userId, prompt.getOnlyProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));
        if (summary.getGroupId() == null) {
            throw new BadRequestException(
                    "You're not part of a group yet, so self-assigned practice isn't available.");
        }

        PromptSubmission submission = promptSubmissionRepository
                .findByStudentAccessIdAndPromptId(summary.getAccessId(), prompt.getId())
                .orElseGet(() -> {
                    PromptSubmission created = new PromptSubmission();
                    created.setStudentAccessId(summary.getAccessId());
                    created.setPromptId(prompt.getId());
                    return created;
                });

        if (submission.getAssignedByAccessId() == null) {
            String championAccessId = accessRepository
                    .findChampionAccessId(prompt.getOnlyProjectId(), summary.getGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException("No champion found for this group"));
            submission.setAssignedByAccessId(championAccessId);
            promptSubmissionRepository.save(submission);
        }

        return getDetails(userId, new PromptDetailsRequest(prompt.getId()));
    }

    @Transactional
    public PromptDetailsResponse submit(UUID userId, PromptSubmitRequest request) {
        Prompt prompt = promptRepository.findById(request.promptId())
                .orElseThrow(() -> new ResourceNotFoundException("Prompt not found"));
        Level level = levelRepository.findById(prompt.getLevelId())
                .orElseThrow(() -> new ResourceNotFoundException("Level not found"));
        Access access = accessRepository.findByUserIdAndProjectId(userId, prompt.getOnlyProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));

        PromptSubmission submission = promptSubmissionRepository
                .findByStudentAccessIdAndPromptId(access.getId(), prompt.getId())
                .filter(s -> s.getAssignedByAccessId() != null)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        if (Boolean.TRUE.equals(submission.getIsAccepted())) {
            throw new BadRequestException("Assignment already accepted, cannot resubmit.");
        }

        submission.setInput(request.input());
        submission.setOutput(request.output());
        submission.setSubmittedAt(OffsetDateTime.now());
        promptEvaluationService.markPending(submission);
        promptSubmissionRepository.save(submission);
        promptEvaluationService.scheduleAfterCommit(submission);

        return getDetails(userId, new PromptDetailsRequest(prompt.getId()));
    }

    private PromptAssignedItem toAssignedItem(PromptAssignedProjection prompt, List<CategorySummary> categories) {
        AssignedByUser assignedBy = prompt.getAssignedById() == null ? null
                : new AssignedByUser(prompt.getAssignedById(), prompt.getAssignedByName(),
                        prompt.getAssignedByAvatar());
        return new PromptAssignedItem(prompt.getId(), prompt.getName(), prompt.getDescription(), prompt.getLevelId(),
                prompt.getLevelNumber(), categories, prompt.getXpReward(), prompt.getIsAccepted(),
                TimeUtils.toOffsetDateTime(prompt.getDeadlineAt()), TimeUtils.toOffsetDateTime(prompt.getReviewedAt()),
                TimeUtils.toOffsetDateTime(prompt.getSubmittedAt()), assignedBy);
    }
}
