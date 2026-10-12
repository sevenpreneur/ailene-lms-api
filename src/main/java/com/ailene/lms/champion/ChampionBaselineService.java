package com.ailene.lms.champion;

import com.ailene.lms.preassessment.PreAssessmentBaseline;
import com.ailene.lms.preassessment.PreAssessmentBaselineReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChampionBaselineService {

    private final ChampionAccessGuard championAccessGuard;
    private final PreAssessmentBaselineReader baselineReader;

    public PreAssessmentTeamResponse getTeamBaseline(String jwt, ChampionProjectRequest request) {
        ChampionContext champion = championAccessGuard.requireChampion(jwt, request.projectId());
        PreAssessmentBaseline baseline = baselineReader.read(request.projectId(), champion.groupId());
        List<PreAssessmentBaseline.Member> group = baseline.members();

        // Scored members weakest band first, then those still being graded, then those who haven't started.
        List<TeamMemberBaseline> members = group.stream()
                .sorted(Comparator.comparingInt(ChampionBaselineService::progressRank)
                        .thenComparing(PreAssessmentBaseline.Member::band,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(PreAssessmentBaseline.Member::average,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .map(m -> {
                    List<PreAssessmentBaseline.Cluster> clusters = baseline.memberClusters(m);
                    return new TeamMemberBaseline(m.accessId(), m.fullName(), m.avatar(), m.status(),
                            baseline.band(m.scored() ? m.band() : null), m.average(), baseline.meetsTarget(m),
                            clusters, PreAssessmentBaseline.weakestCode(clusters),
                            m.accessId().equals(champion.accessId()));
                })
                .toList();

        return new PreAssessmentTeamResponse(champion.groupId(), champion.groupName(), group.size(),
                (int) group.stream().filter(PreAssessmentBaseline.Member::submitted).count(),
                (int) group.stream().filter(PreAssessmentBaseline.Member::scored).count(),
                PreAssessmentBaseline.measuredAt(group), baseline.targetBand(), baseline.meetsTargetCount(group),
                PreAssessmentBaseline.average(group),
                PreAssessmentBaseline.weakestFirst(baseline.averageClusters(group)), baseline.bandCounts(group),
                members);
    }

    private static int progressRank(PreAssessmentBaseline.Member member) {
        if (member.scored()) {
            return 0;
        }
        return member.submitted() ? 1 : 2;
    }
}
