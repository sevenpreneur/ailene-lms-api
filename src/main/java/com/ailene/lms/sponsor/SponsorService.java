package com.ailene.lms.sponsor;

import com.ailene.lms.preassessment.PreAssessment;
import com.ailene.lms.preassessment.PreAssessmentReportBuilder;
import com.ailene.lms.preassessment.PreAssessmentReportPillar;
import com.ailene.lms.preassessment.PreAssessmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.ailene.lms.sponsor.SponsorMetrics.DAY_MONTH;
import static com.ailene.lms.sponsor.SponsorMetrics.ROI_VALUE_PER_HOUR;
import static com.ailene.lms.sponsor.SponsorMetrics.hoursSaved;
import static com.ailene.lms.sponsor.SponsorMetrics.pct;
import static com.ailene.lms.sponsor.SponsorMetrics.round1;
import static com.ailene.lms.sponsor.SponsorMetrics.round2;
import static com.ailene.lms.sponsor.SponsorMetrics.startOfDay;
import static com.ailene.lms.sponsor.SponsorMetrics.startOfWeek;
import static com.ailene.lms.sponsor.SponsorMetrics.today;

@Service
@RequiredArgsConstructor
public class SponsorService {

    private static final String SPONSOR_ROLE = "sponsor";
    private static final String CHAMPION_ROLE = "champion";
    private static final int ACTIVE_WINDOW_DAYS = 7;
    private static final int TREND_WEEKS = 12;
    private static final int ACTIVITY_SOURCE_TAKE = 8;
    private static final int ACTIVITY_TAKE = 6;
    // Program window for the proficiency trend, still a fixed cohort range (no per-project column yet).
    private static final LocalDate PROGRAM_START = LocalDate.of(2026, 6, 26);
    private static final LocalDate PROGRAM_END = LocalDate.of(2026, 8, 26);
    // Maturity line on the org pillar ranking — the 3.2 threshold from the member report.
    private static final double PILLAR_TARGET = 3.2;
    private static final List<String> PILLAR_ORDER = List.of("ai_foundation", "prompting", "tool_fluency",
            "use_case_diversity", "ai_habit", "agentic");

    private final SponsorAccessGuard sponsorAccessGuard;
    private final SponsorRepository sponsorRepository;
    private final PreAssessmentRepository preAssessmentRepository;

    public OrganizationStatsResponse getOrganizationStats(String jwt, SponsorProjectRequest request) {
        String projectId = guard(jwt, request);

        return new OrganizationStatsResponse(sponsorRepository.findMembers(projectId).size(),
                sponsorRepository.findGroups(projectId).size());
    }

    public ExecutiveViewResponse getExecutiveView(String jwt, SponsorProjectRequest request) {
        String projectId = guard(jwt, request);

        List<MemberRowProjection> members = sponsorRepository.findMembers(projectId);
        Instant activeSince = Instant.now().minus(ACTIVE_WINDOW_DAYS, ChronoUnit.DAYS);

        int memberCount = members.size();
        double levelSum = 0;
        int activeWeekly = 0;
        for (MemberRowProjection member : members) {
            levelSum += levelNumber(member);
            if (isActiveSince(member, activeSince)) {
                activeWeekly++;
            }
        }
        double avgLevel = memberCount == 0 ? 0 : levelSum / memberCount;

        double hoursSavedTotal = 0;
        for (UseCaseRowProjection row : sponsorRepository.findUseCaseSubmissions(projectId)) {
            if (row.getSubmittedAt() != null) {
                hoursSavedTotal += hoursSaved(row.getHoursWithoutAi(), row.getHoursWithAi());
            }
        }

        ExecutiveMetrics metrics = new ExecutiveMetrics(round1(avgLevel), memberCount, round1(hoursSavedTotal),
                Math.round(hoursSavedTotal * ROI_VALUE_PER_HOUR), activeWeekly, pct(activeWeekly, memberCount));

        return new ExecutiveViewResponse(metrics);
    }

    public HeadlineResponse getHeadline(String jwt, SponsorProjectRequest request) {
        String projectId = guard(jwt, request);

        List<MemberRowProjection> members = sponsorRepository.findMembers(projectId).stream()
                .filter(member -> !SPONSOR_ROLE.equals(member.getRole()))
                .toList();
        Set<String> memberIds = new HashSet<>();
        members.forEach(member -> memberIds.add(member.getAccessId()));

        LocalDate weekStart = startOfWeek(today());
        List<Instant> bucketStarts = new ArrayList<>();
        List<String> bucketLabels = new ArrayList<>();
        for (int i = 0; i < TREND_WEEKS; i++) {
            LocalDate start = weekStart.minusWeeks(TREND_WEEKS - 1L - i);
            bucketStarts.add(startOfDay(start));
            bucketLabels.add(start.format(DAY_MONTH));
        }
        double[] bucketHours = new double[TREND_WEEKS];

        Instant lastWeekThreshold = Instant.now().minus(ACTIVE_WINDOW_DAYS, ChronoUnit.DAYS);
        Set<String> productiveMemberIds = new HashSet<>();
        double hoursSavedLastWeek = 0;

        for (UseCaseRowProjection row : sponsorRepository.findUseCaseSubmissions(projectId)) {
            if (row.getSubmittedAt() == null || !Boolean.TRUE.equals(row.getAccepted())) {
                continue;
            }
            if (!memberIds.contains(row.getAccessId())) {
                continue;
            }
            double saved = hoursSaved(row.getHoursWithoutAi(), row.getHoursWithAi());
            if (saved <= 0) {
                continue;
            }
            productiveMemberIds.add(row.getAccessId());
            Instant at = row.getSubmittedAt();
            if (at.isAfter(lastWeekThreshold)) {
                hoursSavedLastWeek += saved;
            }
            for (int i = 0; i < TREND_WEEKS; i++) {
                Instant start = bucketStarts.get(i);
                Instant end = start.plus(7, ChronoUnit.DAYS);
                if (!at.isBefore(start) && at.isBefore(end)) {
                    bucketHours[i] += saved;
                    break;
                }
            }
        }

        List<TrendPoint> trend = new ArrayList<>();
        for (int i = 0; i < TREND_WEEKS; i++) {
            trend.add(new TrendPoint(bucketLabels.get(i), round1(bucketHours[i])));
        }

        return new HeadlineResponse(pct(productiveMemberIds.size(), members.size()), productiveMemberIds.size(),
                members.size(), round1(hoursSavedLastWeek),
                Math.round(hoursSavedLastWeek * 52 * ROI_VALUE_PER_HOUR), trend);
    }

    public ProgramHealthResponse getProgramHealth(String jwt, SponsorProjectRequest request) {
        String projectId = guard(jwt, request);

        List<MemberRowProjection> members = sponsorRepository.findMembers(projectId);
        int memberCount = members.size();
        int atL1 = 0;
        int atL2 = 0;
        for (MemberRowProjection member : members) {
            short level = levelNumber(member);
            if (level >= 1) {
                atL1++;
            }
            if (level >= 2) {
                atL2++;
            }
        }

        int reviewedTotal = 0;
        int acceptedTotal = 0;
        Set<String> submitters = new HashSet<>();
        for (UseCaseRowProjection row : sponsorRepository.findUseCaseSubmissions(projectId)) {
            if (row.getReviewedAt() != null) {
                reviewedTotal++;
                if (Boolean.TRUE.equals(row.getAccepted())) {
                    acceptedTotal++;
                }
            }
            if (row.getSubmittedAt() != null) {
                submitters.add(row.getAccessId());
            }
        }
        for (PromptRowProjection row : sponsorRepository.findPromptSubmissions(projectId)) {
            if (row.getReviewedAt() != null) {
                reviewedTotal++;
                if (Boolean.TRUE.equals(row.getAccepted())) {
                    acceptedTotal++;
                }
            }
            if (row.getSubmittedAt() != null) {
                submitters.add(row.getAccessId());
            }
        }

        List<HealthMetric> metrics = List.of(
                new HealthMetric("pass_l1", "Lulus L1", "Capai Level 1+", pct(atL1, memberCount),
                        atL1 + " dari " + memberCount + " staff"),
                new HealthMetric("pass_l2", "Lulus L2", "Capai Level 2+", pct(atL2, memberCount),
                        atL2 + " dari " + memberCount + " staff"),
                new HealthMetric("accepted", "Submission diterima", "Hasil kerja di-ACC",
                        pct(acceptedTotal, reviewedTotal), acceptedTotal + " dari " + reviewedTotal + " direview"),
                new HealthMetric("participation", "Partisipasi", "Staff pernah submit",
                        pct(submitters.size(), memberCount), submitters.size() + " dari " + memberCount + " staff"));

        return new ProgramHealthResponse(metrics);
    }

    public RecentActivityResponse getRecentActivity(String jwt, SponsorProjectRequest request) {
        String projectId = guard(jwt, request);

        List<ActivityItem> items = new ArrayList<>();
        for (ActivityRowProjection row : sponsorRepository.findRecentUseCaseSubmissions(projectId,
                ACTIVITY_SOURCE_TAKE)) {
            items.add(new ActivityItem("submission", actorOr(row.getActor(), "Staff"), "kirim use case",
                    meta(row.getGroupName(), row.getSubject()), relativeTime(row.getAt()), row.getAt()));
        }
        for (ActivityRowProjection row : sponsorRepository.findRecentUseCaseReviews(projectId,
                ACTIVITY_SOURCE_TAKE)) {
            boolean accepted = Boolean.TRUE.equals(row.getAccepted());
            items.add(new ActivityItem(accepted ? "accepted" : "review", actorOr(row.getActor(), "Champion"),
                    accepted ? "terima use case" : "review use case", meta(row.getGroupName(), row.getSubject()),
                    relativeTime(row.getAt()), row.getAt()));
        }
        for (ActivityRowProjection row : sponsorRepository.findRecentPreAssessments(projectId,
                ACTIVITY_SOURCE_TAKE)) {
            items.add(new ActivityItem("assessment", actorOr(row.getActor(), "Staff"), "selesai pre-assessment",
                    meta(row.getGroupName(), row.getSubject()), relativeTime(row.getAt()), row.getAt()));
        }

        List<ActivityItem> activity = items.stream()
                .sorted(Comparator.comparing(ActivityItem::at).reversed())
                .limit(ACTIVITY_TAKE)
                .toList();

        return new RecentActivityResponse(activity);
    }

    public WeeklyTrendsResponse getWeeklyTrends(String jwt, SponsorProjectRequest request) {
        String projectId = guard(jwt, request);

        int totalMembers = sponsorRepository.findMembers(projectId).size();
        LocalDate firstWeek = startOfWeek(today()).minusWeeks(TREND_WEEKS - 1L);
        List<UseCaseRowProjection> rows = sponsorRepository.findUseCaseSubmissions(projectId);

        List<WeeklyTrendPoint> weeks = new ArrayList<>();
        for (int index = 0; index < TREND_WEEKS; index++) {
            LocalDate weekStart = firstWeek.plusWeeks(index);
            Instant start = startOfDay(weekStart);
            Instant end = start.plus(7, ChronoUnit.DAYS);

            Set<String> activeMembers = new HashSet<>();
            double hours = 0;
            for (UseCaseRowProjection row : rows) {
                Instant at = row.getSubmittedAt();
                if (at == null || at.isBefore(start) || !at.isBefore(end)) {
                    continue;
                }
                activeMembers.add(row.getAccessId());
                hours += hoursSaved(row.getHoursWithoutAi(), row.getHoursWithAi());
            }

            weeks.add(new WeeklyTrendPoint(weekStart.format(DAY_MONTH), round1(hours),
                    pct(activeMembers.size(), totalMembers), index == TREND_WEEKS - 1));
        }

        return new WeeklyTrendsResponse(weeks);
    }

    public ProficiencyTrendsResponse getProficiencyTrends(String jwt, SponsorProjectRequest request) {
        String projectId = guard(jwt, request);

        Instant start = startOfDay(PROGRAM_START);
        Instant end = startOfDay(PROGRAM_END.plusDays(1)).minusMillis(1);
        long days = ChronoUnit.DAYS.between(PROGRAM_START, PROGRAM_END.plusDays(1));
        int weekCount = (int) Math.max(1, Math.ceil(days / 7.0));

        List<MemberRowProjection> members = sponsorRepository.findMembers(projectId).stream()
                .filter(member -> !SPONSOR_ROLE.equals(member.getRole()))
                .toList();

        Map<String, List<XpRowProjection>> xpByMember = new HashMap<>();
        for (XpRowProjection row : sponsorRepository.findXpEarnings(projectId)) {
            Instant at = row.getEarnedAt();
            if (at == null || at.isBefore(start) || at.isAfter(end)) {
                continue;
            }
            xpByMember.computeIfAbsent(row.getAccessId(), key -> new ArrayList<>()).add(row);
        }

        Map<String, List<LevelHistoryRowProjection>> unlocksByMember = new HashMap<>();
        for (LevelHistoryRowProjection row : sponsorRepository.findLevelHistory(projectId)) {
            unlocksByMember.computeIfAbsent(row.getAccessId(), key -> new ArrayList<>()).add(row);
        }

        List<ProficiencyPoint> weeks = new ArrayList<>();
        for (int index = 0; index < weekCount; index++) {
            Instant weekEnd = startOfDay(PROGRAM_START.plusWeeks(index + 1L)).minusMillis(1);
            Instant cutoff = weekEnd.isAfter(end) ? end : weekEnd;

            double sumLevel = 0;
            long sumXp = 0;
            int existing = 0;
            for (MemberRowProjection member : members) {
                if (member.getCreatedAt() == null || member.getCreatedAt().isAfter(cutoff)) {
                    continue;
                }
                existing++;

                List<LevelHistoryRowProjection> unlocks = unlocksByMember.getOrDefault(member.getAccessId(),
                        List.of());
                short level = 0;
                if (unlocks.isEmpty()) {
                    level = levelNumber(member);
                } else {
                    for (LevelHistoryRowProjection unlock : unlocks) {
                        short unlocked = unlock.getLevelNumber() == null ? 0 : unlock.getLevelNumber();
                        if (unlock.getReachedAt() != null && !unlock.getReachedAt().isAfter(cutoff)
                                && unlocked > level) {
                            level = unlocked;
                        }
                    }
                }
                sumLevel += level;

                for (XpRowProjection xp : xpByMember.getOrDefault(member.getAccessId(), List.of())) {
                    if (!xp.getEarnedAt().isAfter(cutoff)) {
                        sumXp += xp.getXpEarned() == null ? 0 : xp.getXpEarned();
                    }
                }
            }

            weeks.add(new ProficiencyPoint("M" + (index + 1), existing == 0 ? 0 : round2(sumLevel / existing),
                    existing == 0 ? 0 : (int) Math.round(sumXp / (double) existing), index == weekCount - 1));
        }

        return new ProficiencyTrendsResponse(weeks);
    }

    public LevelDistributionResponse getLevelDistribution(String jwt, SponsorProjectRequest request) {
        String projectId = guard(jwt, request);

        List<MemberRowProjection> members = sponsorRepository.findMembers(projectId);
        List<GroupRowProjection> groups = sponsorRepository.findGroups(projectId);
        Instant activeSince = Instant.now().minus(ACTIVE_WINDOW_DAYS, ChronoUnit.DAYS);

        Map<Short, LevelRowProjection> levelByNumber = new HashMap<>();
        for (LevelRowProjection level : sponsorRepository.findActiveLevels(projectId)) {
            levelByNumber.put(level.getLevelNumber(), level);
        }

        // Level 0..3 are always rendered, even when the project has no row for one of them.
        List<DisplayLevel> displayLevels = new ArrayList<>();
        for (short levelNumber = 0; levelNumber <= 3; levelNumber++) {
            LevelRowProjection level = levelByNumber.get(levelNumber);
            displayLevels.add(new DisplayLevel(level == null ? -levelNumber - 1 : level.getId(), levelNumber,
                    level == null ? "Level " + levelNumber : level.getName()));
        }

        int total = members.size();
        int activeWeekly = 0;
        Map<Integer, Integer> countByLevel = new HashMap<>();
        for (MemberRowProjection member : members) {
            if (isActiveSince(member, activeSince)) {
                activeWeekly++;
            }
            if (member.getLevelId() != null) {
                countByLevel.merge(member.getLevelId(), 1, Integer::sum);
            }
        }

        List<LevelSlice> levels = new ArrayList<>();
        for (DisplayLevel level : displayLevels) {
            int count = countByLevel.getOrDefault(level.id(), 0);
            levels.add(new LevelSlice(level.id(), "L" + level.levelNumber(), "Level " + level.levelNumber(),
                    level.name(), count, pct(count, total)));
        }

        Set<Integer> entryLevelIds = new HashSet<>();
        displayLevels.stream().filter(level -> level.levelNumber() <= 1)
                .forEach(level -> entryLevelIds.add(level.id()));

        List<GroupLevelRow> groupRows = new ArrayList<>();
        for (GroupRowProjection group : groups) {
            List<MemberRowProjection> groupMembers = members.stream()
                    .filter(member -> group.getId().equals(member.getGroupId()))
                    .toList();
            int groupTotal = groupMembers.size();

            List<GroupLevelSlice> groupLevels = new ArrayList<>();
            for (DisplayLevel level : displayLevels) {
                int count = (int) groupMembers.stream()
                        .filter(member -> Integer.valueOf(level.id()).equals(member.getLevelId()))
                        .count();
                groupLevels.add(new GroupLevelSlice(level.id(), "L" + level.levelNumber(),
                        "Level " + level.levelNumber(), level.name(), count, pct(count, groupTotal)));
            }

            int beginnerCount = (int) groupMembers.stream()
                    .filter(member -> member.getLevelId() != null && entryLevelIds.contains(member.getLevelId()))
                    .count();
            int groupActive = (int) groupMembers.stream()
                    .filter(member -> isActiveSince(member, activeSince))
                    .count();

            groupRows.add(new GroupLevelRow(group.getId(), group.getName(), groupTotal, groupActive, beginnerCount,
                    pct(beginnerCount, groupTotal), groupLevels));
        }

        groupRows = groupRows.stream()
                .sorted(Comparator.comparingInt(GroupLevelRow::total).reversed()
                        .thenComparing(GroupLevelRow::name))
                .toList();

        List<GroupLevelRow> needingIntervention = groupRows.stream()
                .filter(group -> group.total() > 0 && group.entryLevelPercent() >= 35)
                .sorted(Comparator.comparingInt(GroupLevelRow::entryLevelPercent).reversed()
                        .thenComparing(Comparator.comparingInt(GroupLevelRow::total).reversed()))
                .toList();

        return new LevelDistributionResponse(total, activeWeekly, pct(activeWeekly, total), levels, groupRows,
                needingIntervention);
    }

    public WorkforceResponse getWorkforceMembers(String jwt, SponsorProjectRequest request) {
        String projectId = guard(jwt, request);

        List<MemberRowProjection> members = sponsorRepository.findMembers(projectId).stream()
                .filter(member -> !SPONSOR_ROLE.equals(member.getRole()))
                .toList();

        long totalTasks = sponsorRepository.countLearningTasks(projectId);
        Map<String, Long> tasksDone = new HashMap<>();
        for (TaskDoneProjection row : sponsorRepository.findTasksDone(projectId)) {
            tasksDone.put(row.getAccessId(), row.getDoneCount() == null ? 0 : row.getDoneCount());
        }

        Instant weekAgo = Instant.now().minus(ACTIVE_WINDOW_DAYS, ChronoUnit.DAYS);
        Map<String, Double> hoursWeekly = new HashMap<>();
        for (UseCaseRowProjection row : sponsorRepository.findUseCaseSubmissions(projectId)) {
            if (row.getSubmittedAt() == null || row.getSubmittedAt().isBefore(weekAgo)) {
                continue;
            }
            double saved = hoursSaved(row.getHoursWithoutAi(), row.getHoursWithAi());
            if (saved > 0) {
                hoursWeekly.merge(row.getAccessId(), saved, Double::sum);
            }
        }

        Map<String, Instant> latestUnlock = new HashMap<>();
        for (LevelHistoryRowProjection row : sponsorRepository.findLevelHistory(projectId)) {
            if (row.getReachedAt() == null) {
                continue;
            }
            latestUnlock.merge(row.getAccessId(), row.getReachedAt(),
                    (current, candidate) -> candidate.isAfter(current) ? candidate : current);
        }

        List<WorkforceMember> list = new ArrayList<>();
        for (MemberRowProjection member : members) {
            long done = tasksDone.getOrDefault(member.getAccessId(), 0L);
            int progressPercent = totalTasks == 0 ? 0
                    : (int) Math.max(0, Math.min(100, Math.round((done * 100.0) / totalTasks)));
            short level = levelNumber(member);
            double score = round1(Math.min(level + progressPercent / 100.0, 4.9));

            String segment = level >= 3 ? "Promotor" : level == 2 ? "Netral" : "Resistor";
            MemberStatus status = buildStatus(member, level, latestUnlock.get(member.getAccessId()));

            list.add(new WorkforceMember(member.getAccessId(),
                    new MemberUserRef(member.getUserId(), member.getFullName(), member.getEmail(),
                            member.getAvatar()),
                    member.getGroupId() == null ? null
                            : new DepartmentRef(member.getGroupId(), member.getGroupName()),
                    member.getJobTitle(),
                    new MemberLevelRef(member.getLevelId(), member.getLevelNumber(), member.getLevelName()), score,
                    progressPercent, segment, round1(hoursWeekly.getOrDefault(member.getAccessId(), 0.0)), status));
        }

        list = list.stream()
                .sorted(Comparator.comparingDouble(WorkforceMember::score).reversed()
                        .thenComparing((WorkforceMember member) -> member.user().fullName()))
                .toList();

        Map<Integer, DepartmentRef> departments = new LinkedHashMap<>();
        list.stream().map(WorkforceMember::department).filter(java.util.Objects::nonNull)
                .forEach(department -> departments.putIfAbsent(department.id(), department));
        List<DepartmentRef> departmentList = departments.values().stream()
                .sorted(Comparator.comparing(DepartmentRef::name))
                .toList();

        return new WorkforceResponse(list.size(), departmentList, list);
    }

    public OrganizationLeaderboardResponse getOrganizationLeaderboard(String jwt, SponsorProjectRequest request) {
        String projectId = guard(jwt, request);

        List<MemberRowProjection> members = sponsorRepository.findMembers(projectId);
        List<GroupRowProjection> groups = sponsorRepository.findGroups(projectId);

        short maxScore = 3;
        for (LevelRowProjection level : sponsorRepository.findActiveLevels(projectId)) {
            if (level.getLevelNumber() != null && level.getLevelNumber() > maxScore) {
                maxScore = level.getLevelNumber();
            }
        }

        Instant weekAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant twoWeeksAgo = Instant.now().minus(14, ChronoUnit.DAYS);
        Map<Integer, GroupAccumulator> accumulators = new HashMap<>();

        for (UseCaseRowProjection row : sponsorRepository.findUseCaseSubmissions(projectId)) {
            if (row.getGroupId() == null || row.getSubmittedAt() == null) {
                continue;
            }
            GroupAccumulator acc = accumulators.computeIfAbsent(row.getGroupId(),
                    key -> new GroupAccumulator());
            acc.submissionCount++;
            if (row.getUseCaseName() != null) {
                acc.useCaseCounts.merge(row.getUseCaseName(), 1, Integer::sum);
            }
            double saved = hoursSaved(row.getHoursWithoutAi(), row.getHoursWithAi());
            if (saved <= 0) {
                continue;
            }
            acc.hoursTotal += saved;
            if (row.getSubmittedAt().isAfter(weekAgo)) {
                acc.hoursThisWeek += saved;
            } else if (row.getSubmittedAt().isAfter(twoWeeksAgo)) {
                acc.hoursPrevWeek += saved;
            }
        }
        for (PromptRowProjection row : sponsorRepository.findPromptSubmissions(projectId)) {
            if (row.getGroupId() == null || row.getSubmittedAt() == null) {
                continue;
            }
            accumulators.computeIfAbsent(row.getGroupId(), key -> new GroupAccumulator()).submissionCount++;
        }

        record Row(int id, String name, int memberCount, double avgScore, String topUseCase, int submissionCount,
                double hours, Integer trendPercent) {
        }

        List<Row> rows = new ArrayList<>();
        for (GroupRowProjection group : groups) {
            GroupAccumulator acc = accumulators.get(group.getId());
            List<MemberRowProjection> groupMembers = members.stream()
                    .filter(member -> group.getId().equals(member.getGroupId()))
                    .toList();

            double levelSum = 0;
            for (MemberRowProjection member : groupMembers) {
                levelSum += levelNumber(member);
            }
            double avgScore = groupMembers.isEmpty() ? 0 : levelSum / groupMembers.size();

            String topUseCase = null;
            if (acc != null) {
                int best = -1;
                for (Map.Entry<String, Integer> entry : acc.useCaseCounts.entrySet()) {
                    if (entry.getValue() > best) {
                        best = entry.getValue();
                        topUseCase = entry.getKey();
                    }
                }
            }

            double hoursThisWeek = acc == null ? 0 : acc.hoursThisWeek;
            double hoursPrevWeek = acc == null ? 0 : acc.hoursPrevWeek;
            Integer trendPercent = hoursPrevWeek > 0
                    ? (int) Math.round(((hoursThisWeek - hoursPrevWeek) / hoursPrevWeek) * 100)
                    : null;

            rows.add(new Row(group.getId(), group.getName(), groupMembers.size(), round1(avgScore), topUseCase,
                    acc == null ? 0 : acc.submissionCount, round1(acc == null ? 0 : acc.hoursTotal), trendPercent));
        }

        List<Row> ranked = rows.stream()
                .sorted(Comparator.comparingDouble(Row::avgScore).reversed().thenComparing(Row::name))
                .toList();

        List<DepartmentPerformance> list = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            Row row = ranked.get(i);
            list.add(new DepartmentPerformance(i + 1, row.id(), row.name(), row.memberCount(), row.avgScore(),
                    row.topUseCase(), row.submissionCount(), row.hours(), row.trendPercent()));
        }

        return new OrganizationLeaderboardResponse(maxScore, list);
    }

    public PreAssessmentOrganizationResponse getPreAssessmentOrganization(String jwt,
            SponsorProjectRequest request) {
        String projectId = guard(jwt, request);

        List<MemberRowProjection> members = sponsorRepository.findMembers(projectId);
        List<GroupRowProjection> groups = sponsorRepository.findGroups(projectId);

        Map<Integer, Integer> groupByPreAssessment = new HashMap<>();
        for (PreAssessmentGroupProjection row : sponsorRepository.findPreAssessmentGroups(projectId)) {
            groupByPreAssessment.put(row.getPreAssessmentId(), row.getGroupId());
        }

        Map<Integer, PillarAccumulator> byGroup = new HashMap<>();
        for (PreAssessment preAssessment : preAssessmentRepository
                .findAllById(groupByPreAssessment.keySet())) {
            Integer groupId = groupByPreAssessment.get(preAssessment.getId());
            if (groupId == null) {
                continue;
            }
            PillarAccumulator acc = byGroup.computeIfAbsent(groupId, key -> new PillarAccumulator());
            for (PreAssessmentReportPillar pillar : PreAssessmentReportBuilder.build(preAssessment).pillars()) {
                acc.sums.merge(pillar.key(), pillar.score(), Double::sum);
            }
            acc.count++;
            Instant createdAt = preAssessment.getCreatedAt() == null ? null
                    : preAssessment.getCreatedAt().toInstant();
            if (createdAt != null && (acc.measuredAt == null || createdAt.isAfter(acc.measuredAt))) {
                acc.measuredAt = createdAt;
            }
        }

        List<DepartmentPillars> departments = new ArrayList<>();
        for (GroupRowProjection group : groups) {
            PillarAccumulator acc = byGroup.get(group.getId());
            if (acc == null) {
                continue;
            }
            List<PillarScore> pillars = PILLAR_ORDER.stream()
                    .map(key -> new PillarScore(key, round1(acc.sums.getOrDefault(key, 0.0) / acc.count)))
                    .toList();
            double avg = round1(pillars.stream().mapToDouble(PillarScore::score).average().orElse(0));
            int memberCount = (int) members.stream()
                    .filter(member -> group.getId().equals(member.getGroupId()))
                    .count();

            departments.add(new DepartmentPillars(group.getId(), group.getName(), memberCount, acc.count,
                    pct(acc.count, memberCount), pillars, avg));
        }

        departments = departments.stream()
                .sorted(Comparator.comparingDouble(DepartmentPillars::avg))
                .toList();

        List<DepartmentPillars> finalDepartments = departments;
        List<PillarScore> orgPillars = PILLAR_ORDER.stream()
                .map(key -> new PillarScore(key, round1(finalDepartments.stream()
                        .mapToDouble(department -> department.pillars().stream()
                                .filter(pillar -> pillar.key().equals(key))
                                .mapToDouble(PillarScore::score)
                                .findFirst()
                                .orElse(0))
                        .average()
                        .orElse(0))))
                .sorted(Comparator.comparingDouble(PillarScore::score))
                .toList();

        double orgAvg = round1(departments.stream().mapToDouble(DepartmentPillars::avg).average().orElse(0));
        int readyCount = (int) departments.stream().filter(d -> d.avg() >= 2.5).count();
        int developingCount = (int) departments.stream().filter(d -> d.avg() >= 1.5 && d.avg() < 2.5).count();
        int basicCount = (int) departments.stream().filter(d -> d.avg() < 1.5).count();
        int gapLargeCount = (int) departments.stream().filter(d -> d.avg() < 2.0).count();

        Instant measuredAt = null;
        for (DepartmentPillars department : departments) {
            Instant candidate = byGroup.get(department.id()).measuredAt;
            if (candidate != null && (measuredAt == null || candidate.isAfter(measuredAt))) {
                measuredAt = candidate;
            }
        }

        return new PreAssessmentOrganizationResponse(departments.size(),
                departments.stream().mapToInt(DepartmentPillars::memberCount).sum(),
                departments.stream().mapToInt(DepartmentPillars::completedCount).sum(), measuredAt, PILLAR_TARGET,
                orgAvg, readyCount, gapLargeCount, departments, orgPillars,
                new Readiness(readyCount, developingCount, basicCount));
    }

    private String guard(String jwt, SponsorProjectRequest request) {
        sponsorAccessGuard.requireSponsor(jwt, request.projectId());
        return request.projectId();
    }

    private MemberStatus buildStatus(MemberRowProjection member, short level, Instant latestUnlock) {
        if (CHAMPION_ROLE.equals(member.getRole())) {
            return new MemberStatus("champion", "Champion aktif");
        }
        if (latestUnlock != null
                && ChronoUnit.DAYS.between(latestUnlock, Instant.now()) <= 14) {
            return new MemberStatus("up", "Naik L" + level + " (" + relativeLabel(latestUnlock) + ")");
        }
        if (level >= 3) {
            return new MemberStatus("pass", "Lulus L" + level + " " + member.getLevelName());
        }
        if (member.getLastActiveAt() == null) {
            return new MemberStatus("idle", "Belum aktif");
        }
        return new MemberStatus("stable", "Stabil di L" + level);
    }

    private String relativeLabel(Instant at) {
        long minutes = Math.max(0, ChronoUnit.MINUTES.between(at, Instant.now()));
        if (minutes < 1) {
            return "baru saja";
        }
        if (minutes < 60) {
            return minutes + " menit lalu";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + " jam lalu";
        }
        return (hours / 24) + " hari lalu";
    }

    private String relativeTime(Instant at) {
        long minutes = Math.max(0, ChronoUnit.MINUTES.between(at, Instant.now()));
        if (minutes < 1) {
            return "baru saja";
        }
        if (minutes < 60) {
            return minutes + " mnt";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "j";
        }
        long days = hours / 24;
        if (days < 7) {
            return days + "h";
        }
        return at.atZone(java.time.ZoneOffset.UTC).toLocalDate().format(DAY_MONTH);
    }

    private static String actorOr(String actor, String fallback) {
        return actor == null || actor.isBlank() ? fallback : actor;
    }

    private static String meta(String groupName, String subject) {
        List<String> parts = new ArrayList<>();
        if (groupName != null && !groupName.isBlank()) {
            parts.add(groupName);
        }
        if (subject != null && !subject.isBlank()) {
            parts.add(subject);
        }
        return String.join(" · ", parts);
    }

    private static short levelNumber(MemberRowProjection member) {
        return member.getLevelNumber() == null ? 0 : member.getLevelNumber();
    }

    private static boolean isActiveSince(MemberRowProjection member, Instant since) {
        return member.getLastActiveAt() != null && !member.getLastActiveAt().isBefore(since);
    }

    private record DisplayLevel(int id, short levelNumber, String name) {
    }

    private static final class GroupAccumulator {
        private double hoursTotal;
        private double hoursThisWeek;
        private double hoursPrevWeek;
        private int submissionCount;
        private final Map<String, Integer> useCaseCounts = new LinkedHashMap<>();
    }

    private static final class PillarAccumulator {
        private final Map<String, Double> sums = new HashMap<>();
        private int count;
        private Instant measuredAt;
    }
}
