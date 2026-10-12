package com.ailene.lms.preassessment;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PreAssessmentBaselineTest {

    private static final List<PreAssessmentBaseline.ClusterRef> CLUSTERS = List.of(
            new PreAssessmentBaseline.ClusterRef("understand", "Memahami AI"),
            new PreAssessmentBaseline.ClusterRef("use", "Memakai AI"));

    private static PreAssessmentBaseline baseline(PreAssessmentBaseline.Member... members) {
        Map<Integer, PreAssessmentBaseline.Band> bands = new LinkedHashMap<>();
        for (int b = 0; b <= 3; b++) {
            bands.put(b, new PreAssessmentBaseline.Band(b, "Band " + b, b));
        }
        return new PreAssessmentBaseline(List.of(members), CLUSTERS, bands, bands.get(2));
    }

    private static PreAssessmentBaseline.Member member(String id, String status, Integer band,
            Map<String, Double> scores) {
        return new PreAssessmentBaseline.Member(id, 1, "Finance", id, null, status, band,
                Instant.parse("2026-10-01T00:00:00Z"), scores);
    }

    @Test
    void onlyScoredMembersCountTowardScoresBandsAndTarget() {
        PreAssessmentBaseline.Member high = member("a", "scored", 3, Map.of("understand", 80.0, "use", 60.0));
        PreAssessmentBaseline.Member low = member("b", "scored", 1, Map.of("understand", 40.0, "use", 20.0));
        PreAssessmentBaseline.Member grading = member("c", "submitted", null, Map.of());
        PreAssessmentBaseline.Member idle = member("d", PreAssessmentBaseline.NOT_STARTED, null, Map.of());
        PreAssessmentBaseline baseline = baseline(high, low, grading, idle);
        List<PreAssessmentBaseline.Member> all = baseline.members();

        assertThat(baseline.averageClusters(all)).extracting(PreAssessmentBaseline.Cluster::score)
                .containsExactly(60.0, 40.0);
        assertThat(PreAssessmentBaseline.average(all)).isEqualTo(50.0);
        assertThat(baseline.meetsTargetCount(all)).isEqualTo(1);
        assertThat(baseline.bandCounts(all)).extracting(PreAssessmentBaseline.BandCount::count)
                .containsExactly(0, 1, 0, 1);
        assertThat(grading.submitted()).isTrue();
        assertThat(idle.submitted()).isFalse();
    }

    @Test
    void unscoredMemberHasNullClustersAndNoWeakest() {
        PreAssessmentBaseline.Member grading = member("c", "submitted", null, Map.of());
        PreAssessmentBaseline baseline = baseline(grading);

        List<PreAssessmentBaseline.Cluster> clusters = baseline.memberClusters(grading);

        assertThat(clusters).extracting(PreAssessmentBaseline.Cluster::score).containsOnlyNulls();
        assertThat(PreAssessmentBaseline.weakestCode(clusters)).isNull();
        assertThat(baseline.averageClusters(baseline.members())).extracting(PreAssessmentBaseline.Cluster::score)
                .containsOnlyNulls();
    }

    @Test
    void weakestFirstPutsUnmeasuredClustersLast() {
        List<PreAssessmentBaseline.Cluster> sorted = PreAssessmentBaseline.weakestFirst(List.of(
                new PreAssessmentBaseline.Cluster("a", "A", null),
                new PreAssessmentBaseline.Cluster("b", "B", 70.0),
                new PreAssessmentBaseline.Cluster("c", "C", 30.0)));

        assertThat(sorted).extracting(PreAssessmentBaseline.Cluster::code).containsExactly("c", "b", "a");
    }
}
