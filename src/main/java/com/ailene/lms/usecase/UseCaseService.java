package com.ailene.lms.usecase;

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
public class UseCaseService {

    private static final short SELF_CREATE_LEVEL_NUMBER = 3;

    private final UseCaseRepository useCaseRepository;
    private final UseCaseSubmissionRepository useCaseSubmissionRepository;
    private final AccessRepository accessRepository;
    private final LevelRepository levelRepository;

    public PagedResponse<UseCaseListItem> list(UUID userId, UseCaseListRequest request) {
        int page = Pagination.normalizePage(request.page());
        int pageSize = Pagination.normalizePageSize(request.pageSize());
        String search = request.search() == null ? "" : request.search();

        long total = useCaseRepository.countUseCases(request.projectId(), search);
        List<UseCaseListProjection> useCases = total == 0
                ? List.of()
                : useCaseRepository.findUseCasePage(request.projectId(), search, pageSize,
                        Pagination.offset(page, pageSize));

        List<Integer> useCaseIds = useCases.stream().map(UseCaseListProjection::getId).toList();

        Map<Integer, List<CategorySummary>> categoriesByUseCase = useCaseIds.isEmpty()
                ? Map.of()
                : useCaseRepository.findCategoriesForUseCases(useCaseIds).stream()
                        .collect(Collectors.groupingBy(UseCaseCategoryProjection::getUseCaseId,
                                Collectors.mapping(c -> new CategorySummary(c.getId(), c.getName()),
                                        Collectors.toList())));

        Map<Integer, UseCaseSubmissionProjection> submissionByUseCase = new HashMap<>();
        if (!useCaseIds.isEmpty()) {
            accessRepository.findByUserIdAndProjectId(userId, request.projectId())
                    .ifPresent(access -> useCaseRepository.findSubmissionsForAccess(access.getId(), useCaseIds)
                            .forEach(submission -> submissionByUseCase.put(submission.getUseCaseId(), submission)));
        }

        List<UseCaseListItem> items = useCases.stream()
                .map(useCase -> toItem(useCase, categoriesByUseCase.get(useCase.getId()),
                        submissionByUseCase.get(useCase.getId())))
                .toList();

        PageMeta meta = Pagination.meta(total, page, pageSize);
        return new PagedResponse<>(items, meta);
    }

    private UseCaseListItem toItem(UseCaseListProjection useCase, List<CategorySummary> categories,
            UseCaseSubmissionProjection submission) {
        return new UseCaseListItem(useCase.getId(), useCase.getName(), useCase.getDescription(),
                useCase.getLevelNumber(), categories == null ? List.of() : categories,
                TimeUtils.toOffsetDateTime(submission == null ? null : submission.getDeadlineAt()),
                TimeUtils.toOffsetDateTime(submission == null ? null : submission.getSubmittedAt()),
                TimeUtils.toOffsetDateTime(submission == null ? null : submission.getReviewedAt()),
                submission == null ? null : submission.getIsAccepted());
    }

    public List<UseCaseAssignedItem> listAssigned(UUID userId, UseCaseAssignedRequest request) {
        Access access = accessRepository.findByUserIdAndProjectId(userId, request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));

        List<UseCaseAssignedProjection> assigned = useCaseRepository.findAssignedUseCases(request.projectId(),
                access.getId(), request.hasSubmitted(), request.isAccepted());

        List<Integer> useCaseIds = assigned.stream().map(UseCaseAssignedProjection::getId).toList();
        Map<Integer, List<CategorySummary>> categoriesByUseCase = useCaseIds.isEmpty()
                ? Map.of()
                : useCaseRepository.findCategoriesForUseCases(useCaseIds).stream()
                        .collect(Collectors.groupingBy(UseCaseCategoryProjection::getUseCaseId,
                                Collectors.mapping(c -> new CategorySummary(c.getId(), c.getName()),
                                        Collectors.toList())));

        return assigned.stream()
                .map(useCase -> toAssignedItem(useCase, categoriesByUseCase.getOrDefault(useCase.getId(), List.of())))
                .toList();
    }

    public UseCaseDetailsResponse getDetails(UUID userId, UseCaseDetailsRequest request) {
        UseCase useCase = useCaseRepository.findById(request.id())
                .orElseThrow(() -> new ResourceNotFoundException("Use case not found"));
        Level level = levelRepository.findById(useCase.getLevelId())
                .orElseThrow(() -> new ResourceNotFoundException("Level not found"));
        Access access = accessRepository.findByUserIdAndProjectId(userId, useCase.getOnlyProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));

        List<CategorySummary> categories = useCaseRepository.findCategoriesForUseCases(List.of(useCase.getId()))
                .stream()
                .map(c -> new CategorySummary(c.getId(), c.getName()))
                .toList();

        UseCaseSubmissionProjection submission = useCaseRepository
                .findSubmissionsForAccess(access.getId(), List.of(useCase.getId())).stream()
                .findFirst()
                .orElse(null);

        return new UseCaseDetailsResponse(useCase.getId(), useCase.getName(), useCase.getDescription(),
                level.getId(), level.getLevelNumber(), categories, useCase.getXpReward(), useCase.getIsSelfCreated(),
                TimeUtils.toOffsetDateTime(submission == null ? null : submission.getDeadlineAt()),
                TimeUtils.toOffsetDateTime(submission == null ? null : submission.getSubmittedAt()),
                TimeUtils.toOffsetDateTime(submission == null ? null : submission.getReviewedAt()),
                submission == null ? null : submission.getIsAccepted());
    }

    @Transactional
    public UseCaseDetailsResponse selfCreate(UUID userId, UseCaseSelfCreateRequest request) {
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
        if (useCaseRepository.countExistingCategories(categoryIds) != categoryIds.size()) {
            throw new ResourceNotFoundException("Some categories were not found");
        }

        Level level = levelRepository.findByLevelNumber(SELF_CREATE_LEVEL_NUMBER)
                .orElseThrow(() -> new ResourceNotFoundException("Use case level (L3) not found"));

        UseCase useCase = new UseCase();
        useCase.setLevelId(level.getId());
        useCase.setOnlyProjectId(request.projectId());
        useCase.setName(request.name());
        useCase.setDescription(request.description());
        useCase.setStatus(Status.active);
        useCase.setIsSelfCreated(true);
        Integer useCaseId = useCaseRepository.save(useCase).getId();

        for (Short categoryId : categoryIds) {
            useCaseRepository.insertCategory(useCaseId, categoryId);
        }

        UseCaseSubmission submission = new UseCaseSubmission();
        submission.setStudentAccessId(summary.getAccessId());
        submission.setUseCaseId(useCaseId);
        submission.setAssignedByAccessId(championAccessId);
        submission.setOutcomeProof(request.outcomeProof());
        submission.setHoursWithAi(request.hoursWithAi());
        submission.setHoursWithoutAi(request.hoursWithoutAi());
        submission.setDescription(request.description());
        submission.setAiTool(request.aiTool());
        submission.setFrequency(request.frequency());
        submission.setType(request.type());
        submission.setSubmittedAt(OffsetDateTime.now());
        useCaseSubmissionRepository.save(submission);

        return getDetails(userId, new UseCaseDetailsRequest(useCaseId));
    }

    @Transactional
    public UseCaseDetailsResponse selfAssign(UUID userId, UseCaseSelfAssignRequest request) {
        UseCase useCase = useCaseRepository.findById(request.useCaseId())
                .filter(u -> u.getStatus() == Status.active)
                .orElseThrow(() -> new ResourceNotFoundException("Use case not found"));
        Level level = levelRepository.findById(useCase.getLevelId())
                .orElseThrow(() -> new ResourceNotFoundException("Level not found"));

        GroupSummaryProjection summary = accessRepository.findGroupSummary(userId, useCase.getOnlyProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));
        if (summary.getGroupId() == null) {
            throw new BadRequestException(
                    "You're not part of a group yet, so self-assigned practice isn't available.");
        }

        UseCaseSubmission submission = useCaseSubmissionRepository
                .findByStudentAccessIdAndUseCaseId(summary.getAccessId(), useCase.getId())
                .orElseGet(() -> {
                    UseCaseSubmission created = new UseCaseSubmission();
                    created.setStudentAccessId(summary.getAccessId());
                    created.setUseCaseId(useCase.getId());
                    return created;
                });

        if (submission.getAssignedByAccessId() == null) {
            String championAccessId = accessRepository
                    .findChampionAccessId(useCase.getOnlyProjectId(), summary.getGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException("No champion found for this group"));
            submission.setAssignedByAccessId(championAccessId);
            useCaseSubmissionRepository.save(submission);
        }

        return getDetails(userId, new UseCaseDetailsRequest(useCase.getId()));
    }

    @Transactional
    public UseCaseDetailsResponse submit(UUID userId, UseCaseSubmitRequest request) {
        UseCase useCase = useCaseRepository.findById(request.useCaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Use case not found"));
        Level level = levelRepository.findById(useCase.getLevelId())
                .orElseThrow(() -> new ResourceNotFoundException("Level not found"));
        Access access = accessRepository.findByUserIdAndProjectId(userId, useCase.getOnlyProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));

        UseCaseSubmission submission = useCaseSubmissionRepository
                .findByStudentAccessIdAndUseCaseId(access.getId(), useCase.getId())
                .filter(s -> s.getAssignedByAccessId() != null)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        if (Boolean.TRUE.equals(submission.getIsAccepted())) {
            throw new BadRequestException("Assignment already accepted, cannot resubmit.");
        }

        submission.setOutcomeProof(request.outcomeProof());
        submission.setHoursWithAi(request.hoursWithAi());
        submission.setHoursWithoutAi(request.hoursWithoutAi());
        submission.setDescription(request.description());
        submission.setAiTool(request.aiTool());
        submission.setFrequency(request.frequency());
        submission.setType(request.type());
        submission.setSubmittedAt(OffsetDateTime.now());
        useCaseSubmissionRepository.save(submission);

        return getDetails(userId, new UseCaseDetailsRequest(useCase.getId()));
    }

    private UseCaseAssignedItem toAssignedItem(UseCaseAssignedProjection useCase, List<CategorySummary> categories) {
        AssignedByUser assignedBy = useCase.getAssignedById() == null ? null
                : new AssignedByUser(useCase.getAssignedById(), useCase.getAssignedByName(),
                        useCase.getAssignedByAvatar());
        return new UseCaseAssignedItem(useCase.getId(), useCase.getName(), useCase.getDescription(),
                useCase.getLevelId(), useCase.getLevelNumber(), categories, useCase.getXpReward(),
                useCase.getIsAccepted(), TimeUtils.toOffsetDateTime(useCase.getDeadlineAt()),
                TimeUtils.toOffsetDateTime(useCase.getReviewedAt()),
                TimeUtils.toOffsetDateTime(useCase.getSubmittedAt()), assignedBy);
    }
}
