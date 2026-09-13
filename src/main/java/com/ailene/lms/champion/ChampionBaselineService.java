package com.ailene.lms.champion;

import com.ailene.lms.preassessment.PreAssessment;
import com.ailene.lms.preassessment.PreAssessmentReportBuilder;
import com.ailene.lms.preassessment.PreAssessmentReportSummary;
import com.ailene.lms.preassessment.PreAssessmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChampionBaselineService {

    // Maturity line on the ranking -- the 3.2 threshold from the member report.
    private static final double PILLAR_TARGET = 3.2;
    private static final List<String> PILLAR_ORDER = List.of("ai_foundation", "prompting", "tool_fluency",
            "use_case_diversity", "ai_habit", "agentic");

    private final ChampionAccessGuard championAccessGuard;
    private final ChampionRepository championRepository;
    private final PreAssessmentRepository preAssessmentRepository;

    public PreAssessmentTeamResponse getTeamBaseline(String jwt, ChampionProjectRequest request) {
        ChampionContext champion = championAccessGuard.requireChampion(jwt, request.projectId());

        int totalMembers = championRepository
                .findTeamMembers(request.projectId(), champion.groupId(), champion.accessId()).size();

        List<TeamPreAssessmentProjection> rows = championRepository
                .findTeamPreAssessments(request.projectId(), champion.groupId());
        if (rows.isEmpty()) {
            return emptyResponse(totalMembers);
        }

        Map<Integer, TeamPreAssessmentProjection> byId = new HashMap<>();
        rows.forEach(row -> byId.put(row.getPreAssessmentId(), row));

        List<TeamMemberBaseline> members = new ArrayList<>();
        Instant measuredAt = null;
        for (PreAssessment preAssessment : preAssessmentRepository.findAllById(byId.keySet())) {
            TeamPreAssessmentProjection row = byId.get(preAssessment.getId());
            if (row == null || row.getAccessId().equals(champion.accessId())) {
                continue;
            }
            PreAssessmentReportSummary report = PreAssessmentReportBuilder.build(preAssessment);
            List<TeamPillarScore> pillars = report.pillars().stream()
                    .map(pillar -> new TeamPillarScore(pillar.key(), pillar.score()))
                    .toList();

            members.add(new TeamMemberBaseline(row.getAccessId(), row.getFullName(), row.getAvatar(),
                    report.avg(), pillars, report.weakest().key()));

            Instant createdAt = preAssessment.getCreatedAt() == null ? null
                    : preAssessment.getCreatedAt().toInstant();
            if (createdAt != null && (measuredAt == null || createdAt.isAfter(measuredAt))) {
                measuredAt = createdAt;
            }
        }

        if (members.isEmpty()) {
            return emptyResponse(totalMembers);
        }

        List<TeamMemberBaseline> sortedMembers = members.stream()
                .sorted(Comparator.comparingDouble(TeamMemberBaseline::avg))
                .toList();

        List<TeamPillarScore> departmentPillars = PILLAR_ORDER.stream()
                .map(key -> new TeamPillarScore(key, round1(averagePillar(sortedMembers, key))))
                .toList();
        double departmentAvg = round1(
                sortedMembers.stream().mapToDouble(TeamMemberBaseline::avg).average().orElse(0));

        TeamDepartmentBaseline department = new TeamDepartmentBaseline(champion.groupId(), champion.groupName(),
                totalMembers, sortedMembers.size(), pct(sortedMembers.size(), totalMembers), departmentAvg,
                departmentPillars, sortedMembers);

        List<TeamPillarScore> teamPillars = PILLAR_ORDER.stream()
                .map(key -> new TeamPillarScore(key, round1(averagePillar(sortedMembers, key))))
                .sorted(Comparator.comparingDouble(TeamPillarScore::score))
                .toList();

        int ready = (int) sortedMembers.stream().filter(member -> member.avg() >= 2.5).count();
        int developing = (int) sortedMembers.stream()
                .filter(member -> member.avg() >= 1.5 && member.avg() < 2.5).count();
        int basic = (int) sortedMembers.stream().filter(member -> member.avg() < 1.5).count();
        int gapLarge = (int) sortedMembers.stream().filter(member -> member.avg() < 2.0).count();

        return new PreAssessmentTeamResponse(1, totalMembers, sortedMembers.size(), measuredAt, PILLAR_TARGET,
                departmentAvg, ready, gapLarge, List.of(department), teamPillars,
                new TeamReadiness(ready, developing, basic));
    }

    private PreAssessmentTeamResponse emptyResponse(int totalMembers) {
        List<TeamPillarScore> pillars = PILLAR_ORDER.stream()
                .map(key -> new TeamPillarScore(key, 0))
                .toList();
        return new PreAssessmentTeamResponse(0, totalMembers, 0, null, PILLAR_TARGET, 0, 0, 0, List.of(), pillars,
                new TeamReadiness(0, 0, 0));
    }

    private static double averagePillar(List<TeamMemberBaseline> members, String key) {
        return members.stream()
                .mapToDouble(member -> member.pillars().stream()
                        .filter(pillar -> pillar.key().equals(key))
                        .mapToDouble(TeamPillarScore::score)
                        .findFirst()
                        .orElse(0))
                .average()
                .orElse(0);
    }

    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private static int pct(int count, int total) {
        return total == 0 ? 0 : (int) Math.round((count * 100.0) / total);
    }
}
