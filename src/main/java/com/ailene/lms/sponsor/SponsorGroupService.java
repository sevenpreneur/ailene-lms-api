package com.ailene.lms.sponsor;

import com.ailene.lms.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ailene.lms.sponsor.SponsorMetrics.hoursSaved;
import static com.ailene.lms.sponsor.SponsorMetrics.pct;
import static com.ailene.lms.sponsor.SponsorMetrics.round1;
import static com.ailene.lms.sponsor.SponsorMetrics.startOfDay;
import static com.ailene.lms.sponsor.SponsorMetrics.startOfMonth;
import static com.ailene.lms.sponsor.SponsorMetrics.today;

@Service
@RequiredArgsConstructor
public class SponsorGroupService {

    private static final String SPONSOR_ROLE = "sponsor";
    private static final int ACTIVE_WINDOW_DAYS = 7;
    private static final int INTERVENTION_THRESHOLD_PERCENT = 35;
    private static final int TOP_USE_CASE_LIMIT = 5;

    private final SponsorAccessGuard sponsorAccessGuard;
    private final SponsorRepository sponsorRepository;

    public DepartmentListResponse getDepartments(String jwt, SponsorProjectRequest request) {
        sponsorAccessGuard.requireSponsor(jwt, request.projectId());
        String projectId = request.projectId();

        Map<Integer, Integer> memberCounts = new HashMap<>();
        for (MemberRowProjection member : sponsorRepository.findMembers(projectId)) {
            if (member.getGroupId() != null) {
                memberCounts.merge(member.getGroupId(), 1, Integer::sum);
            }
        }

        List<DepartmentItem> departments = sponsorRepository.findGroups(projectId).stream()
                .map(group -> new DepartmentItem(group.getId(), group.getName(),
                        memberCounts.getOrDefault(group.getId(), 0)))
                .toList();

        return new DepartmentListResponse(departments);
    }

    public GroupOverviewResponse getOverview(String jwt, SponsorGroupRequest request) {
        String projectId = guard(jwt, request);
        GroupRowProjection group = requireGroup(projectId, request.groupId());

        List<MemberRowProjection> members = groupMembers(projectId, request.groupId());
        Instant activeSince = Instant.now().minus(ACTIVE_WINDOW_DAYS, ChronoUnit.DAYS);
        Instant monthStart = startOfDay(startOfMonth(today()));

        int totalMembers = members.size();
        int activeMembers = 0;
        double levelSum = 0;
        int beginnerCount = 0;
        for (MemberRowProjection member : members) {
            short level = levelNumber(member);
            levelSum += level;
            if (level <= 1) {
                beginnerCount++;
            }
            if (member.getLastActiveAt() != null && !member.getLastActiveAt().isBefore(activeSince)) {
                activeMembers++;
            }
        }
        double avgLevel = totalMembers == 0 ? 0 : round1(levelSum / totalMembers);

        int acceptedUseCases = 0;
        int acceptedThisMonth = 0;
        double hoursSavedTotal = 0;
        for (UseCaseRowProjection row : groupUseCaseSubmissions(projectId, request.groupId())) {
            if (row.getSubmittedAt() == null || !Boolean.TRUE.equals(row.getAccepted())) {
                continue;
            }
            acceptedUseCases++;
            hoursSavedTotal += hoursSaved(row.getHoursWithoutAi(), row.getHoursWithAi());
            if (row.getSubmittedAt().isAfter(monthStart)) {
                acceptedThisMonth++;
            }
        }

        int beginnerPercent = pct(beginnerCount, totalMembers);
        GroupMetrics metrics = new GroupMetrics(totalMembers, activeMembers, pct(activeMembers, totalMembers),
                avgLevel, beginnerCount, beginnerPercent, round1(hoursSavedTotal), acceptedUseCases,
                acceptedThisMonth, totalMembers > 0 && beginnerPercent >= INTERVENTION_THRESHOLD_PERCENT);

        ChampionInfo champion = sponsorRepository.findGroupChampion(projectId, request.groupId())
                .map(row -> new ChampionInfo(row.getAccessId(), row.getFullName(), row.getAvatar(),
                        row.getJobTitle()))
                .orElse(null);

        return new GroupOverviewResponse(new GroupInfo(group.getId(), group.getName(), champion), metrics);
    }

    public GroupLevelDistributionResponse getLevelDistribution(String jwt, SponsorGroupRequest request) {
        String projectId = guard(jwt, request);
        requireGroup(projectId, request.groupId());

        List<MemberRowProjection> members = groupMembers(projectId, request.groupId());
        Map<Integer, Integer> countByLevel = new HashMap<>();
        for (MemberRowProjection member : members) {
            if (member.getLevelId() != null) {
                countByLevel.merge(member.getLevelId(), 1, Integer::sum);
            }
        }

        List<GroupLevelItem> levels = sponsorRepository.findActiveLevels(projectId).stream()
                .map(level -> {
                    int count = countByLevel.getOrDefault(level.getId(), 0);
                    return new GroupLevelItem(level.getId(), level.getLevelNumber(),
                            "L" + level.getLevelNumber(), level.getName(), count, pct(count, members.size()));
                })
                .toList();

        return new GroupLevelDistributionResponse(members.size(), levels);
    }

    public GroupTopUseCasesResponse getTopUseCases(String jwt, SponsorGroupRequest request) {
        String projectId = guard(jwt, request);
        requireGroup(projectId, request.groupId());

        Map<Integer, UseCaseAccumulator> byUseCase = new HashMap<>();
        int total = 0;
        for (UseCaseRowProjection row : groupUseCaseSubmissions(projectId, request.groupId())) {
            if (row.getSubmittedAt() == null || !Boolean.TRUE.equals(row.getAccepted())) {
                continue;
            }
            total++;
            UseCaseAccumulator acc = byUseCase.computeIfAbsent(row.getUseCaseId(), key -> new UseCaseAccumulator());
            acc.name = row.getUseCaseName();
            acc.levelCode = row.getLevelNumber() == null ? null : "L" + row.getLevelNumber();
            acc.levelName = row.getLevelName();
            acc.count++;
        }

        int finalTotal = total;
        List<TopUseCaseItem> useCases = byUseCase.entrySet().stream()
                .map(entry -> new TopUseCaseItem(entry.getKey(), entry.getValue().name, entry.getValue().levelCode,
                        entry.getValue().levelName, entry.getValue().count, pct(entry.getValue().count, finalTotal)))
                .sorted(Comparator.comparingInt(TopUseCaseItem::count).reversed()
                        .thenComparing(TopUseCaseItem::name))
                .limit(TOP_USE_CASE_LIMIT)
                .toList();

        return new GroupTopUseCasesResponse(total, useCases);
    }

    public AttentionMembersResponse getAttentionMembers(String jwt, SponsorGroupRequest request) {
        String projectId = guard(jwt, request);
        requireGroup(projectId, request.groupId());

        Instant now = Instant.now();
        Instant activeSince = now.minus(ACTIVE_WINDOW_DAYS, ChronoUnit.DAYS);

        Map<String, Integer> acceptedByMember = new HashMap<>();
        for (UseCaseRowProjection row : groupUseCaseSubmissions(projectId, request.groupId())) {
            if (row.getSubmittedAt() == null) {
                continue;
            }
            if (Boolean.TRUE.equals(row.getAccepted())) {
                acceptedByMember.merge(row.getAccessId(), 1, Integer::sum);
            }
        }

        List<AttentionMember> rows = new ArrayList<>();
        for (MemberRowProjection member : groupMembers(projectId, request.groupId())) {
            if (SPONSOR_ROLE.equals(member.getRole())) {
                continue;
            }
            Long inactiveDays = member.getLastActiveAt() == null ? null
                    : ChronoUnit.DAYS.between(member.getLastActiveAt(), now);
            int acceptedCount = acceptedByMember.getOrDefault(member.getAccessId(), 0);
            short level = levelNumber(member);
            boolean inactive = member.getLastActiveAt() == null || member.getLastActiveAt().isBefore(activeSince);
            boolean needsAttention = level <= 1 || acceptedCount == 0 || inactive;

            String status = member.getLastActiveAt() == null ? "Belum mulai"
                    : member.getLastActiveAt().isBefore(activeSince) ? "Pasif " + inactiveDays + " hari" : "Aktif";
            int score = (level <= 1 ? 3 : 0) + (acceptedCount == 0 ? 2 : 0) + (inactive ? 2 : 0);

            rows.add(new AttentionMember(member.getAccessId(), member.getFullName(), member.getAvatar(),
                    member.getJobTitle(), level, member.getLevelName(), acceptedCount, inactiveDays, status,
                    needsAttention, score));
        }

        Comparator<AttentionMember> byAttention = (a, b) -> Boolean.compare(b.needsAttention(), a.needsAttention());
        List<AttentionMember> sorted = rows.stream()
                .sorted(byAttention
                        .thenComparing(Comparator.comparingInt(AttentionMember::score).reversed())
                        .thenComparing(Comparator.comparingInt(AttentionMember::levelNumber))
                        .thenComparing(AttentionMember::fullName))
                .toList();

        long laggingCount = sorted.stream().filter(AttentionMember::needsAttention).count();

        return new AttentionMembersResponse((int) laggingCount, sorted);
    }

    private String guard(String jwt, SponsorGroupRequest request) {
        sponsorAccessGuard.requireSponsor(jwt, request.projectId());
        return request.projectId();
    }

    private GroupRowProjection requireGroup(String projectId, Integer groupId) {
        return sponsorRepository.findGroups(projectId).stream()
                .filter(group -> group.getId().equals(groupId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Group not found in this project"));
    }

    private List<MemberRowProjection> groupMembers(String projectId, Integer groupId) {
        return sponsorRepository.findMembers(projectId).stream()
                .filter(member -> groupId.equals(member.getGroupId()))
                .toList();
    }

    private List<UseCaseRowProjection> groupUseCaseSubmissions(String projectId, Integer groupId) {
        return sponsorRepository.findUseCaseSubmissions(projectId).stream()
                .filter(row -> groupId.equals(row.getGroupId()))
                .toList();
    }

    private static short levelNumber(MemberRowProjection member) {
        return member.getLevelNumber() == null ? 0 : member.getLevelNumber();
    }

    private static final class UseCaseAccumulator {
        private String name;
        private String levelCode;
        private String levelName;
        private int count;
    }
}
