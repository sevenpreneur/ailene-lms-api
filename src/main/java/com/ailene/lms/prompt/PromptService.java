package com.ailene.lms.prompt;

import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.common.CategorySummary;
import com.ailene.lms.common.TimeUtils;
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
public class PromptService {

    private final PromptRepository promptRepository;
    private final AccessRepository accessRepository;

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
                submission == null ? null : submission.getIsAccepted());
    }
}
