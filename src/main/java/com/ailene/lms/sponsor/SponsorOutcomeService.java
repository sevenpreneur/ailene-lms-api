package com.ailene.lms.sponsor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ailene.lms.sponsor.SponsorMetrics.MONTH_YEAR;
import static com.ailene.lms.sponsor.SponsorMetrics.ROI_VALUE_PER_HOUR;
import static com.ailene.lms.sponsor.SponsorMetrics.WORK_HOURS_PER_YEAR;
import static com.ailene.lms.sponsor.SponsorMetrics.hoursSaved;
import static com.ailene.lms.sponsor.SponsorMetrics.pct;
import static com.ailene.lms.sponsor.SponsorMetrics.round1;
import static com.ailene.lms.sponsor.SponsorMetrics.startOfDay;
import static com.ailene.lms.sponsor.SponsorMetrics.startOfMonth;
import static com.ailene.lms.sponsor.SponsorMetrics.today;

@Service
@RequiredArgsConstructor
public class SponsorOutcomeService {

    private static final String SPONSOR_ROLE = "sponsor";
    private static final int ROI_TREND_MONTHS = 6;
    // The last two months of the ROI trend are projected from the actual growth step.
    private static final int ROI_ACTUAL_MONTHS = 4;

    private final SponsorAccessGuard sponsorAccessGuard;
    private final SponsorRepository sponsorRepository;

    public OutcomeOverviewResponse getOverview(String jwt, SponsorProjectRequest request) {
        String projectId = guard(jwt, request);

        List<MemberRowProjection> members = sponsorRepository.findMembers(projectId);
        int memberCount = members.size();

        double levelSum = 0;
        int certifiedCount = 0;
        for (MemberRowProjection member : members) {
            short level = levelNumber(member);
            levelSum += level;
            if (level >= 1) {
                certifiedCount++;
            }
        }
        double avgLevel = memberCount == 0 ? 0 : levelSum / memberCount;

        double hoursSavedTotal = 0;
        for (UseCaseRowProjection row : sponsorRepository.findUseCaseSubmissions(projectId)) {
            if (row.getSubmittedAt() != null) {
                hoursSavedTotal += hoursSaved(row.getHoursWithoutAi(), row.getHoursWithAi());
            }
        }

        short maxLevelNumber = 0;
        for (LevelRowProjection level : sponsorRepository.findActiveLevels()) {
            if (level.getLevelNumber() != null && level.getLevelNumber() > maxLevelNumber) {
                maxLevelNumber = level.getLevelNumber();
            }
        }

        return new OutcomeOverviewResponse(memberCount, sponsorRepository.findGroups(projectId).size(),
                round1(hoursSavedTotal), round1(hoursSavedTotal / WORK_HOURS_PER_YEAR),
                Math.round(hoursSavedTotal * ROI_VALUE_PER_HOUR), ROI_VALUE_PER_HOUR, round1(avgLevel),
                maxLevelNumber, certifiedCount, pct(certifiedCount, memberCount));
    }

    public OutcomeLevelDistributionResponse getLevelDistribution(String jwt, SponsorProjectRequest request) {
        String projectId = guard(jwt, request);

        List<MemberRowProjection> members = sponsorRepository.findMembers(projectId);
        Map<Short, Integer> countByLevel = new HashMap<>();
        for (MemberRowProjection member : members) {
            countByLevel.merge(levelNumber(member), 1, Integer::sum);
        }

        List<OutcomeLevelItem> distribution = sponsorRepository.findActiveLevels().stream()
                .map(level -> {
                    int count = countByLevel.getOrDefault(level.getLevelNumber(), 0);
                    return new OutcomeLevelItem(level.getLevelNumber(), "L" + level.getLevelNumber(),
                            level.getName(), count, pct(count, members.size()));
                })
                .toList();

        return new OutcomeLevelDistributionResponse(members.size(), distribution);
    }

    public RoiTrendResponse getRoiTrend(String jwt, SponsorProjectRequest request) {
        String projectId = guard(jwt, request);

        LocalDate firstMonth = startOfMonth(today()).minusMonths(ROI_TREND_MONTHS - 1L);
        List<UseCaseRowProjection> rows = sponsorRepository.findUseCaseSubmissions(projectId);

        long[] values = new long[ROI_TREND_MONTHS];
        String[] labels = new String[ROI_TREND_MONTHS];
        for (int index = 0; index < ROI_TREND_MONTHS; index++) {
            LocalDate month = firstMonth.plusMonths(index);
            Instant start = startOfDay(month);
            Instant end = startOfDay(month.plusMonths(1));

            double hours = 0;
            for (UseCaseRowProjection row : rows) {
                Instant at = row.getSubmittedAt();
                if (at == null || at.isBefore(start) || !at.isBefore(end)) {
                    continue;
                }
                hours += hoursSaved(row.getHoursWithoutAi(), row.getHoursWithAi());
            }
            values[index] = Math.round(hours * ROI_VALUE_PER_HOUR);
            labels[index] = month.format(MONTH_YEAR);
        }

        // Everything past the last actual month is extrapolated from the observed growth step.
        int lastActualIndex = ROI_ACTUAL_MONTHS - 1;
        double growthStep = lastActualIndex <= 0 ? values[0] * 0.18
                : (values[lastActualIndex] - values[0]) / (double) lastActualIndex;
        for (int index = lastActualIndex + 1; index < ROI_TREND_MONTHS; index++) {
            values[index] = Math.max(values[index - 1], Math.round(values[index - 1] + growthStep));
        }

        List<RoiMonth> months = new ArrayList<>();
        for (int index = 0; index < ROI_TREND_MONTHS; index++) {
            months.add(new RoiMonth("M" + (index + 1), labels[index], values[index],
                    round1(values[index] / 1_000_000_000.0), index >= ROI_ACTUAL_MONTHS));
        }

        return new RoiTrendResponse(months);
    }

    public DepartmentRoiResponse getDepartmentRoi(String jwt, SponsorProjectRequest request) {
        String projectId = guard(jwt, request);

        List<MemberRowProjection> members = sponsorRepository.findMembers(projectId);
        Instant weekAgo = Instant.now().minus(7, ChronoUnit.DAYS);

        Map<Integer, double[]> hoursByGroup = new HashMap<>();
        for (UseCaseRowProjection row : sponsorRepository.findUseCaseSubmissions(projectId)) {
            if (row.getGroupId() == null || row.getSubmittedAt() == null) {
                continue;
            }
            double saved = hoursSaved(row.getHoursWithoutAi(), row.getHoursWithAi());
            double[] acc = hoursByGroup.computeIfAbsent(row.getGroupId(), key -> new double[2]);
            acc[0] += saved;
            if (!row.getSubmittedAt().isBefore(weekAgo)) {
                acc[1] += saved;
            }
        }

        List<DepartmentRoiItem> rows = new ArrayList<>();
        long totalRoi = 0;
        for (GroupRowProjection group : sponsorRepository.findGroups(projectId)) {
            double[] acc = hoursByGroup.getOrDefault(group.getId(), new double[2]);
            int memberCount = (int) members.stream()
                    .filter(member -> group.getId().equals(member.getGroupId()))
                    .count();
            long roiAnnualized = Math.round(acc[1] * 52 * ROI_VALUE_PER_HOUR);
            totalRoi += roiAnnualized;

            rows.add(new DepartmentRoiItem(group.getId(), group.getName(), memberCount, round1(acc[1]),
                    round1(acc[0]), roiAnnualized, 0));
        }

        long finalTotalRoi = totalRoi;
        List<DepartmentRoiItem> departments = rows.stream()
                .map(row -> new DepartmentRoiItem(row.id(), row.name(), row.memberCount(), row.hoursSavedWeekly(),
                        row.hoursSavedTotal(), row.roiAnnualized(), pct(row.roiAnnualized(), finalTotalRoi)))
                .sorted(Comparator.comparingDouble(DepartmentRoiItem::hoursSavedWeekly).reversed()
                        .thenComparing(Comparator.comparingLong(DepartmentRoiItem::roiAnnualized).reversed())
                        .thenComparing(DepartmentRoiItem::name))
                .toList();

        return new DepartmentRoiResponse(totalRoi, departments);
    }

    public TopPerformersResponse getTopPerformers(String jwt, SponsorProjectRequest request) {
        String projectId = guard(jwt, request);

        List<MemberRowProjection> members = sponsorRepository.findMembers(projectId).stream()
                .filter(member -> !SPONSOR_ROLE.equals(member.getRole()))
                .toList();

        Map<String, Integer> xpByMember = new HashMap<>();
        for (XpRowProjection row : sponsorRepository.findXpEarnings(projectId)) {
            xpByMember.merge(row.getAccessId(), row.getXpEarned() == null ? 0 : row.getXpEarned(), Integer::sum);
        }

        Map<String, Integer> useCaseCountByMember = new HashMap<>();
        Map<String, Double> hoursByMember = new HashMap<>();
        for (UseCaseRowProjection row : sponsorRepository.findUseCaseSubmissions(projectId)) {
            if (row.getSubmittedAt() == null) {
                continue;
            }
            useCaseCountByMember.merge(row.getAccessId(), 1, Integer::sum);
            hoursByMember.merge(row.getAccessId(), hoursSaved(row.getHoursWithoutAi(), row.getHoursWithAi()),
                    Double::sum);
        }

        short maxLevelNumber = 1;
        for (LevelRowProjection level : sponsorRepository.findActiveLevels()) {
            if (level.getLevelNumber() != null && level.getLevelNumber() > maxLevelNumber) {
                maxLevelNumber = level.getLevelNumber();
            }
        }

        record Base(String accessId, String fullName, String avatar, String department, short levelNumber,
                String levelName, int xp, int useCaseCount, double hours) {
        }

        List<Base> base = new ArrayList<>();
        for (MemberRowProjection member : members) {
            base.add(new Base(member.getAccessId(), member.getFullName(), member.getAvatar(),
                    member.getGroupName() == null ? "—" : member.getGroupName(), levelNumber(member),
                    member.getLevelName(), xpByMember.getOrDefault(member.getAccessId(), 0),
                    useCaseCountByMember.getOrDefault(member.getAccessId(), 0),
                    round1(hoursByMember.getOrDefault(member.getAccessId(), 0.0))));
        }

        // Each signal is normalized against the org max so the leader scores full marks on it.
        int maxXp = Math.max(1, base.stream().mapToInt(Base::xp).max().orElse(1));
        double maxHours = Math.max(1, base.stream().mapToDouble(Base::hours).max().orElse(1));
        int maxUseCases = Math.max(1, base.stream().mapToInt(Base::useCaseCount).max().orElse(1));
        short finalMaxLevelNumber = maxLevelNumber;

        record Scored(Base base, int composite) {
        }

        List<Scored> scored = base.stream()
                .map(row -> new Scored(row, (int) Math.round(100 * (0.4 * (row.xp() / (double) maxXp)
                        + 0.25 * (row.hours() / maxHours)
                        + 0.2 * (row.useCaseCount() / (double) maxUseCases)
                        + 0.15 * (row.levelNumber() / (double) finalMaxLevelNumber)))))
                .sorted(Comparator.comparingInt(Scored::composite).reversed()
                        .thenComparing(Comparator.comparingDouble((Scored row) -> row.base().hours()).reversed())
                        .thenComparing((Scored row) -> row.base().fullName()))
                .toList();

        List<PerformerItem> list = new ArrayList<>();
        for (int i = 0; i < scored.size(); i++) {
            Base row = scored.get(i).base();
            list.add(new PerformerItem(i + 1, row.accessId(), row.fullName(), row.avatar(), row.department(),
                    row.levelNumber(), "L" + row.levelNumber(), row.levelName(), row.xp(), row.useCaseCount(),
                    row.hours(), scored.get(i).composite()));
        }

        return new TopPerformersResponse(list.size(), list);
    }

    private String guard(String jwt, SponsorProjectRequest request) {
        sponsorAccessGuard.requireSponsor(jwt, request.projectId());
        return request.projectId();
    }

    private static short levelNumber(MemberRowProjection member) {
        return member.getLevelNumber() == null ? 0 : member.getLevelNumber();
    }
}
