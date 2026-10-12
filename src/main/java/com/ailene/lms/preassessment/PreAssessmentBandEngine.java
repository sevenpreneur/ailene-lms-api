package com.ailene.lms.preassessment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

// Pure CFTE scoring: item credits -> p1/p2/p3 -> raw band -> security and behaviour gates -> level.
final class PreAssessmentBandEngine {

    static final String KNOWLEDGE = "knowledge";
    static final String SKILLS = "skills";
    static final String BEHAVIOURS = "behaviours";
    private static final String PROFILE = "profile";
    private static final String TEXT = "text";

    record Item(int id, String section, String type, Integer domainId, String dimension, Integer cfteLevel,
            boolean critical, boolean behaviourGate, Integer overclaimItemId, Map<Integer, Integer> levelThresholds) {
    }

    record Answer(int itemId, BigDecimal credit, Integer rubricTotal, boolean sensitiveData) {
    }

    record Config(int securityGateMaxBand, int behaviourGateFromBand, int behaviourGateMaxBand,
            double behaviourGateMinScore, double behaviourGateMinCredit, double overclaimMinCredit,
            double overclaimCappedCredit) {
    }

    record Band(int band, int level, BigDecimal minP1, BigDecimal minP2, BigDecimal minP3) {
    }

    record DomainScore(int domainId, BigDecimal score, int itemCount) {
    }

    record Result(BigDecimal p1, BigDecimal p2, BigDecimal p3, BigDecimal knowledge, BigDecimal skills,
            BigDecimal behaviours, int rawBand, int finalBand, int level, boolean securityGatePassed,
            boolean behaviourGatePassed, boolean overclaim, List<DomainScore> domainScores) {
    }

    private record Unit(Integer domainId, String dimension, Integer level, double credit) {
    }

    private PreAssessmentBandEngine() {
    }

    static Result score(List<Item> items, Map<Integer, Answer> answers, Config config, List<Band> bands) {
        List<Unit> units = new ArrayList<>();
        List<Double> behaviourChoices = new ArrayList<>();
        List<Double> behaviourTexts = new ArrayList<>();
        List<Double> behaviourGateCredits = new ArrayList<>();
        boolean securityPassed = true;
        boolean overclaim = false;

        for (Item item : items) {
            if (PROFILE.equals(item.section())) {
                continue;
            }
            Answer answer = answers.get(item.id());
            if (answer != null && answer.sensitiveData()) {
                securityPassed = false;
            }

            if (!item.levelThresholds().isEmpty()) {
                int total = answer == null || answer.rubricTotal() == null ? 0 : answer.rubricTotal();
                item.levelThresholds().forEach((level, min) ->
                        units.add(new Unit(item.domainId(), item.dimension(), level, total >= min ? 1 : 0)));
                continue;
            }

            double credit = credit(answers, item.id());
            if (item.critical() && credit < 1) {
                securityPassed = false;
            }
            if (BEHAVIOURS.equals(item.dimension())) {
                if (item.overclaimItemId() != null && credit >= config.overclaimMinCredit()
                        && credit(answers, item.overclaimItemId()) < 1) {
                    credit = config.overclaimCappedCredit();
                    overclaim = true;
                }
                (TEXT.equals(item.type()) ? behaviourTexts : behaviourChoices).add(credit);
                if (item.behaviourGate()) {
                    behaviourGateCredits.add(credit);
                }
            }
            units.add(new Unit(item.domainId(), item.dimension(), item.cfteLevel(), credit));
        }

        double p1 = percent(units, u -> Integer.valueOf(1).equals(u.level()));
        double p2 = percent(units, u -> Integer.valueOf(2).equals(u.level()));
        double p3 = percent(units, u -> Integer.valueOf(3).equals(u.level()));
        double knowledge = percent(units, u -> KNOWLEDGE.equals(u.dimension()));
        double skills = percent(units, u -> SKILLS.equals(u.dimension()));
        double behaviours = behaviourScore(behaviourChoices, behaviourTexts);

        int rawBand = bands.stream()
                .sorted(Comparator.comparingInt(Band::band).reversed())
                .filter(b -> meets(p1, b.minP1()) && meets(p2, b.minP2()) && meets(p3, b.minP3()))
                .mapToInt(Band::band)
                .findFirst()
                .orElse(0);

        int finalBand = rawBand;
        if (!securityPassed) {
            finalBand = Math.min(finalBand, config.securityGateMaxBand());
        }
        boolean behaviourPassed = behaviours >= config.behaviourGateMinScore()
                && behaviourGateCredits.stream().allMatch(c -> c >= config.behaviourGateMinCredit());
        if (finalBand >= config.behaviourGateFromBand() && !behaviourPassed) {
            finalBand = Math.min(finalBand, config.behaviourGateMaxBand());
        }
        int bandForLevel = finalBand;
        int level = bands.stream().filter(b -> b.band() == bandForLevel).mapToInt(Band::level).findFirst().orElse(0);

        Map<Integer, List<Double>> byDomain = new LinkedHashMap<>();
        units.stream().filter(u -> u.domainId() != null)
                .forEach(u -> byDomain.computeIfAbsent(u.domainId(), k -> new ArrayList<>()).add(u.credit()));
        List<DomainScore> domainScores = byDomain.entrySet().stream()
                .map(e -> new DomainScore(e.getKey(), round(average(e.getValue()) * 100), e.getValue().size()))
                .toList();

        return new Result(round(p1), round(p2), round(p3), round(knowledge), round(skills), round(behaviours),
                rawBand, finalBand, level, securityPassed, behaviourPassed, overclaim, domainScores);
    }

    private static double credit(Map<Integer, Answer> answers, int itemId) {
        Answer answer = answers.get(itemId);
        return answer == null || answer.credit() == null ? 0 : answer.credit().doubleValue();
    }

    private static double percent(List<Unit> units, Predicate<Unit> filter) {
        return average(units.stream().filter(filter).map(Unit::credit).toList()) * 100;
    }

    private static double behaviourScore(List<Double> choices, List<Double> texts) {
        if (choices.isEmpty() || texts.isEmpty()) {
            return average(choices.isEmpty() ? texts : choices) * 100;
        }
        return (average(choices) * 0.5 + average(texts) * 0.5) * 100;
    }

    private static double average(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private static boolean meets(double value, BigDecimal min) {
        return min == null || value + 1e-9 >= min.doubleValue();
    }

    private static BigDecimal round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
