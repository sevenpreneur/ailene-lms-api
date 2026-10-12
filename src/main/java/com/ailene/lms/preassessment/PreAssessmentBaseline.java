package com.ailene.lms.preassessment;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;

public record PreAssessmentBaseline(List<Member> members, List<ClusterRef> clusters, Map<Integer, Band> bands,
        Band targetBand) {

    public static final String NOT_STARTED = "not_started";
    public static final String SCORED = "scored";

    public record Member(String accessId, Integer groupId, String groupName, String fullName, String avatar,
            String status, Integer band, Instant submittedAt, Map<String, Double> clusterScores) {

        public boolean submitted() {
            return !NOT_STARTED.equals(status);
        }

        public boolean scored() {
            return SCORED.equals(status) && band != null;
        }

        // Mean of the member's cluster scores; null until scored.
        public Double average() {
            if (!scored() || clusterScores.isEmpty()) {
                return null;
            }
            return round1(clusterScores.values().stream().mapToDouble(Double::doubleValue).average().orElse(0));
        }
    }

    public record ClusterRef(String code, String name) {
    }

    public record Band(Integer band, String name, Integer level) {
    }

    public record BandCount(Integer band, String name, Integer level, int count) {
    }

    public record Cluster(String code, String name, Double score) {
    }

    public Band band(Integer band) {
        return band == null ? null : bands.getOrDefault(band, new Band(band, null, null));
    }

    public boolean meetsTarget(Member member) {
        return member.scored() && member.band() >= targetBand.band();
    }

    // In cluster order; a cluster the member has no score for stays null.
    public List<Cluster> memberClusters(Member member) {
        return clusters.stream()
                .map(c -> new Cluster(c.code(), c.name(), member.scored() && member.clusterScores().containsKey(c.code())
                        ? round1(member.clusterScores().get(c.code()))
                        : null))
                .toList();
    }

    // Plain mean over the scored members; null for a cluster nobody has a score in.
    public List<Cluster> averageClusters(List<Member> group) {
        return clusters.stream()
                .map(c -> {
                    OptionalDouble avg = group.stream()
                            .filter(Member::scored)
                            .map(m -> m.clusterScores().get(c.code()))
                            .filter(Objects::nonNull)
                            .mapToDouble(Double::doubleValue)
                            .average();
                    return new Cluster(c.code(), c.name(), avg.isPresent() ? round1(avg.getAsDouble()) : null);
                })
                .toList();
    }

    // Every configured band, including empty ones, so a distribution chart keeps its axis.
    public List<BandCount> bandCounts(List<Member> group) {
        return bands.values().stream()
                .map(b -> new BandCount(b.band(), b.name(), b.level(), (int) group.stream()
                        .filter(m -> m.scored() && b.band().equals(m.band()))
                        .count()))
                .toList();
    }

    public int meetsTargetCount(List<Member> group) {
        return (int) group.stream().filter(this::meetsTarget).count();
    }

    public static Double average(List<Member> group) {
        OptionalDouble avg = group.stream()
                .map(Member::average)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average();
        return avg.isPresent() ? round1(avg.getAsDouble()) : null;
    }

    public static Instant measuredAt(List<Member> group) {
        return group.stream()
                .map(Member::submittedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    public static String weakestCode(List<Cluster> clusters) {
        return clusters.stream()
                .filter(c -> c.score() != null)
                .min(Comparator.comparingDouble(Cluster::score))
                .map(Cluster::code)
                .orElse(null);
    }

    public static List<Cluster> weakestFirst(List<Cluster> clusters) {
        return clusters.stream()
                .sorted(Comparator.comparing(Cluster::score, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public static int pct(int count, int total) {
        return total == 0 ? 0 : (int) Math.round((count * 100.0) / total);
    }

    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
