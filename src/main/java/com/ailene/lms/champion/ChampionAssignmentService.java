package com.ailene.lms.champion;

import com.ailene.lms.common.Status;
import com.ailene.lms.common.exception.BadRequestException;
import com.ailene.lms.common.exception.ForbiddenException;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import com.ailene.lms.level.Level;
import com.ailene.lms.level.LevelRepository;
import com.ailene.lms.notification.NotificationWriter;
import com.ailene.lms.prompt.Prompt;
import com.ailene.lms.prompt.PromptRepository;
import com.ailene.lms.usecase.UseCase;
import com.ailene.lms.usecase.UseCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChampionAssignmentService {

    private static final short PROMPT_LEVEL_NUMBER = 2;
    private static final short USE_CASE_LEVEL_NUMBER = 3;

    private final ChampionAccessGuard championAccessGuard;
    private final ChampionRepository championRepository;
    private final PromptRepository promptRepository;
    private final UseCaseRepository useCaseRepository;
    private final LevelRepository levelRepository;
    private final AssignmentDraftRepository assignmentDraftRepository;
    private final NotificationWriter notificationWriter;

    @Transactional
    public AssignmentResult assignPrompt(String jwt, AssignLibraryRequest request) {
        ChampionContext champion = championAccessGuard.requireChampion(jwt, request.projectId());
        requireFutureDeadline(request.deadline());

        Prompt prompt = promptRepository.findById(request.libraryId())
                .filter(p -> p.getStatus() == Status.active)
                .orElseThrow(() -> new ResourceNotFoundException("Prompt not found"));

        List<String> targets = resolveTargets(champion, request.targetType(), request.targetAccessIds(),
                request.targetGroupIds());

        int assigned = assignPromptTo(targets, prompt.getId(), prompt.getName(), champion, request.deadline(),
                request.message());

        return new AssignmentResult(assigned, targets.size(), targets.size() - assigned);
    }

    @Transactional
    public AssignmentResult assignUseCase(String jwt, AssignLibraryRequest request) {
        ChampionContext champion = championAccessGuard.requireChampion(jwt, request.projectId());
        requireFutureDeadline(request.deadline());

        UseCase useCase = useCaseRepository.findById(request.libraryId())
                .filter(u -> u.getStatus() == Status.active)
                .orElseThrow(() -> new ResourceNotFoundException("Use case not found"));

        List<String> targets = resolveTargets(champion, request.targetType(), request.targetAccessIds(),
                request.targetGroupIds());

        int assigned = assignUseCaseTo(targets, useCase.getId(), useCase.getName(), champion, request.deadline(),
                request.message());

        return new AssignmentResult(assigned, targets.size(), targets.size() - assigned);
    }

    @Transactional
    public CreatePromptAssignmentResponse createPromptAssignment(String jwt,
            CreatePromptAssignmentRequest request) {
        ChampionContext champion = championAccessGuard.requireChampion(jwt, request.projectId());
        AssignmentDraft draft = findOwnDraft(champion, request.draftId(), AssignmentKind.PROMPT);

        List<Short> categoryIds = request.categoryIds().stream().distinct().toList();
        if (promptRepository.countExistingCategories(categoryIds) != categoryIds.size()) {
            throw new ResourceNotFoundException("Some categories were not found");
        }

        Level level = levelRepository.findByLevelNumber(PROMPT_LEVEL_NUMBER)
                .orElseThrow(() -> new ResourceNotFoundException("Prompt level (L2) not found"));

        List<String> targets = resolveAssignmentSpec(champion, request.assignment());

        Prompt prompt = new Prompt();
        prompt.setLevelId(level.getId());
        prompt.setOnlyProjectId(request.projectId());
        prompt.setName(request.name());
        prompt.setScenario(request.description());
        prompt.setExpectedOutput(request.expectedOutput());
        prompt.setStatus(Status.active);
        prompt.setIsSelfCreated(false);
        Integer promptId = promptRepository.save(prompt).getId();
        if (draft != null) {
            draft.setUsedAt(OffsetDateTime.now());
            draft.setUsedPromptId(promptId);
        }

        for (Short categoryId : categoryIds) {
            promptRepository.insertCategory(promptId, categoryId);
        }

        int assigned = request.assignment() == null ? 0
                : assignPromptTo(targets, promptId, request.name(), champion, request.assignment().deadline(),
                        request.assignment().message());

        return new CreatePromptAssignmentResponse(promptId, assigned, targets.size(), targets.size() - assigned);
    }

    @Transactional
    public CreateUseCaseAssignmentResponse createUseCaseAssignment(String jwt,
            CreateUseCaseAssignmentRequest request) {
        ChampionContext champion = championAccessGuard.requireChampion(jwt, request.projectId());
        AssignmentDraft draft = findOwnDraft(champion, request.draftId(), AssignmentKind.USE_CASE);

        List<Short> categoryIds = request.categoryIds().stream().distinct().toList();
        if (useCaseRepository.countExistingCategories(categoryIds) != categoryIds.size()) {
            throw new ResourceNotFoundException("Some categories were not found");
        }

        Level level = levelRepository.findByLevelNumber(USE_CASE_LEVEL_NUMBER)
                .orElseThrow(() -> new ResourceNotFoundException("Use case level (L3) not found"));

        List<String> targets = resolveAssignmentSpec(champion, request.assignment());

        UseCase useCase = new UseCase();
        useCase.setLevelId(level.getId());
        useCase.setOnlyProjectId(request.projectId());
        useCase.setName(request.name());
        useCase.setDescription(request.description());
        useCase.setStatus(Status.active);
        useCase.setIsSelfCreated(false);
        Integer useCaseId = useCaseRepository.save(useCase).getId();
        if (draft != null) {
            draft.setUsedAt(OffsetDateTime.now());
            draft.setUsedUseCaseId(useCaseId);
        }

        for (Short categoryId : categoryIds) {
            useCaseRepository.insertCategory(useCaseId, categoryId);
        }

        int assigned = request.assignment() == null ? 0
                : assignUseCaseTo(targets, useCaseId, request.name(), champion, request.assignment().deadline(),
                        request.assignment().message());

        return new CreateUseCaseAssignmentResponse(useCaseId, assigned, targets.size(), targets.size() - assigned);
    }

    // Only members who did not already hold this item get a row, and only they are notified.
    private int assignPromptTo(List<String> targets, Integer promptId, String promptName, ChampionContext champion,
            OffsetDateTime deadline, String message) {
        int assigned = 0;
        for (String accessId : targets) {
            int inserted = championRepository.insertPromptAssignment(accessId, promptId, champion.accessId(),
                    deadline, trimToNull(message));
            if (inserted > 0) {
                notificationWriter.promptAssigned(accessId, promptId, promptName, deadline);
            }
            assigned += inserted;
        }
        return assigned;
    }

    private int assignUseCaseTo(List<String> targets, Integer useCaseId, String useCaseName,
            ChampionContext champion, OffsetDateTime deadline, String message) {
        int assigned = 0;
        for (String accessId : targets) {
            int inserted = championRepository.insertUseCaseAssignment(accessId, useCaseId, champion.accessId(),
                    deadline, trimToNull(message));
            if (inserted > 0) {
                notificationWriter.useCaseAssigned(accessId, useCaseId, useCaseName, deadline);
            }
            assigned += inserted;
        }
        return assigned;
    }

    // A used draft can be turned into another library item; used_* then points at the latest one.
    private AssignmentDraft findOwnDraft(ChampionContext champion, Integer draftId, AssignmentKind kind) {
        if (draftId == null) {
            return null;
        }
        AssignmentDraft draft = assignmentDraftRepository.findByIdAndChampionAccessId(draftId, champion.accessId())
                .orElseThrow(() -> new ResourceNotFoundException("Draft not found"));
        if (draft.getKind() != kind) {
            throw new BadRequestException(kind == AssignmentKind.PROMPT ? "Draft is not a prompt draft"
                    : "Draft is not a use case draft");
        }
        return draft;
    }

    private List<String> resolveAssignmentSpec(ChampionContext champion, AssignmentSpec assignment) {
        if (assignment == null) {
            return List.of();
        }
        requireFutureDeadline(assignment.deadline());
        return resolveTargets(champion, assignment.targetType(), assignment.targetAccessIds(),
                assignment.targetGroupIds());
    }

    // A champion leads exactly one group per project, so GROUP targeting can only ever mean that group.
    private List<String> resolveTargets(ChampionContext champion, AssignmentTargetType targetType,
            List<String> targetAccessIds, List<Integer> targetGroupIds) {
        List<String> memberIds;

        if (targetType == AssignmentTargetType.GROUP) {
            if (targetGroupIds == null || targetGroupIds.isEmpty()) {
                throw new BadRequestException("target_group_ids is required when target_type is GROUP");
            }
            if (targetGroupIds.stream().anyMatch(id -> !champion.groupId().equals(id))) {
                throw new ForbiddenException("Some groups are not yours");
            }
            memberIds = championRepository
                    .findTeamMembers(champion.projectId(), champion.groupId(), champion.accessId()).stream()
                    .map(TeamMemberProjection::getAccessId)
                    .toList();
        } else {
            if (targetAccessIds == null || targetAccessIds.isEmpty()) {
                throw new BadRequestException("target_access_ids is required when target_type is MEMBER");
            }
            memberIds = targetAccessIds.stream().distinct().toList();
            long inGroup = championRepository.countAccessesInGroup(champion.projectId(), memberIds,
                    champion.groupId());
            if (inGroup != memberIds.size()) {
                throw new ForbiddenException("Some members are not in the group you lead");
            }
        }

        if (memberIds.isEmpty()) {
            throw new BadRequestException("No target members");
        }
        return memberIds;
    }

    private static void requireFutureDeadline(OffsetDateTime deadline) {
        if (!deadline.isAfter(OffsetDateTime.now())) {
            throw new BadRequestException("Deadline must be in the future");
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
