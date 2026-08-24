package com.ailene.lms.usecase;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.common.AssignedByUser;
import com.ailene.lms.common.CategorySummary;
import com.ailene.lms.common.TimeUtils;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import com.ailene.lms.common.pagination.PageMeta;
import com.ailene.lms.common.pagination.PagedResponse;
import com.ailene.lms.common.pagination.Pagination;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UseCaseService {

    private final UseCaseRepository useCaseRepository;
    private final AccessRepository accessRepository;

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

    private UseCaseAssignedItem toAssignedItem(UseCaseAssignedProjection useCase, List<CategorySummary> categories) {
        AssignedByUser assignedBy = useCase.getAssignedById() == null ? null
                : new AssignedByUser(useCase.getAssignedById(), useCase.getAssignedByName(),
                        useCase.getAssignedByAvatar());
        return new UseCaseAssignedItem(useCase.getId(), useCase.getName(), useCase.getDescription(), categories,
                useCase.getXpReward(), useCase.getIsAccepted(), TimeUtils.toOffsetDateTime(useCase.getDeadlineAt()),
                TimeUtils.toOffsetDateTime(useCase.getReviewedAt()),
                TimeUtils.toOffsetDateTime(useCase.getSubmittedAt()), assignedBy);
    }
}
