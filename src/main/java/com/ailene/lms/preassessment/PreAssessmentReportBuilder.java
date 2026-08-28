package com.ailene.lms.preassessment;

import java.util.List;
import java.util.Map;

public final class PreAssessmentReportBuilder {

    private static final String FALLBACK_QUOTE = "Tiap akhir bulan saya merangkum sekitar 30 exit interview jadi satu laporan untuk manajer, biasanya makan waktu hampir seharian.";

    private static final Map<String, Double> FREQUENCY_SCORE = Map.of("never", 1.0, "rarely", 1.7, "sometimes", 2.5,
            "often", 3.3, "always", 4.1);

    private static final Map<String, Double> AI_USE_FREQUENCY_SCORE = Map.of("never", 1.0, "tried", 1.6, "weekly",
            2.4, "daily", 3.3, "intensive", 4.1);

    private static final Map<String, Double> OUTPUT_REVIEW_SCORE = Map.of("no_check", 1.0, "sometimes", 2.0,
            "always", 3.3, "cross_check", 4.2, "no_use", 1.0);

    private static final Map<String, Double> TEAM_ADOPTION_SCORE = Map.of("none", 1.0, "personal", 1.8, "pilot", 2.6,
            "policy", 3.4, "integrated", 4.2);

    private static final Map<String, Double> PROMPT_SKILL_SCORE = Map.of("none", 1.0, "basic", 1.8, "decent", 2.6,
            "structured", 3.5, "expert", 4.3);

    private static final Map<String, Double> REFINE_SCENARIO_SCORE = Map.of("targeted", 4.2, "manual", 3.2,
            "switch_tool", 2.4, "restart", 1.8);

    private static final Map<String, Double> PROFESSIONAL_ATTITUDE_SCORE = Map.of("too_risky", 1.0, "cautious", 2.7,
            "neutral", 2.2, "supportive", 3.4, "essential", 3.8);

    private PreAssessmentReportBuilder() {
    }

    public static PreAssessmentReportSummary build(PreAssessment pa) {
        double limitationScore = listBreadthScore(pa.getAiLimitations(), "belum tahu");
        double toolBreadthScore = listBreadthScore(pa.getAiToolsUsed(), "belum pernah");
        int useCaseCount = pa.getUseCases() == null ? 0 : pa.getUseCases().length;
        boolean hasConcreteExample = pa.getConcreteExample() != null && !pa.getConcreteExample().isBlank();
        double useCaseDiversityScore = clampScore(1 + useCaseCount * 0.3 + (hasConcreteExample ? 0.3 : 0));

        PreAssessmentReportPillar aiFoundation = pillar("ai_foundation", "AI Foundation",
                avgScore(limitationScore, safeScore(OUTPUT_REVIEW_SCORE, pa.getOutputReview().name()),
                        safeScore(PROFESSIONAL_ATTITUDE_SCORE, pa.getProfessionalAttitude().name()),
                        safeScore(FREQUENCY_SCORE, pa.getDataSafetyCheck().name()),
                        reverseFrequencyScore(pa.getPublishUnchecked().name())));

        PreAssessmentReportPillar prompting = pillar("prompting", "Prompting",
                avgScore(safeScore(PROMPT_SKILL_SCORE, pa.getPromptComfort().name()),
                        safeScore(FREQUENCY_SCORE, pa.getPromptIteration().name()),
                        safeScore(REFINE_SCENARIO_SCORE, pa.getRefineScenario().name())));

        PreAssessmentReportPillar toolFluency = pillar("tool_fluency", "Tool Fluency",
                avgScore(safeScore(AI_USE_FREQUENCY_SCORE, pa.getAiUseFrequency().name()), toolBreadthScore,
                        safeScore(FREQUENCY_SCORE, pa.getModelSelection().name()),
                        safeScore(FREQUENCY_SCORE, pa.getMultimodalUse().name())));

        PreAssessmentReportPillar useCaseDiversity = pillar("use_case_diversity", "Use Case", useCaseDiversityScore);

        PreAssessmentReportPillar aiHabit = pillar("ai_habit", "AI Habit",
                avgScore(safeScore(AI_USE_FREQUENCY_SCORE, pa.getAiUseFrequency().name()),
                        safeScore(FREQUENCY_SCORE, pa.getWorkflowReuse().name()),
                        safeScore(TEAM_ADOPTION_SCORE, pa.getTeamAdoption().name()),
                        safeScore(FREQUENCY_SCORE, pa.getDataSafetyCheck().name())));

        PreAssessmentReportPillar agentic = pillar("agentic", "Agentic",
                avgScore(safeScore(FREQUENCY_SCORE, pa.getWorkflowReuse().name()),
                        safeScore(FREQUENCY_SCORE, pa.getModelSelection().name()),
                        safeScore(FREQUENCY_SCORE, pa.getMultimodalUse().name()),
                        safeScore(TEAM_ADOPTION_SCORE, pa.getTeamAdoption().name())));

        List<PreAssessmentReportPillar> pillars = List.of(aiFoundation, prompting, toolFluency, useCaseDiversity,
                aiHabit, agentic);

        double avg = round1(pillars.stream().mapToDouble(PreAssessmentReportPillar::score).average().orElse(0));

        PreAssessmentReportPillar strongest = pillars.get(0);
        PreAssessmentReportPillar weakest = pillars.get(0);
        for (PreAssessmentReportPillar p : pillars) {
            if (p.score() > strongest.score()) {
                strongest = p;
            }
            if (p.score() < weakest.score()) {
                weakest = p;
            }
        }

        String quote = hasConcreteExample ? pa.getConcreteExample().trim()
                : !pa.getBiggestChallenge().isBlank() ? pa.getBiggestChallenge() : FALLBACK_QUOTE;

        return new PreAssessmentReportSummary(pillars, avg,
                new PreAssessmentPillarRef(strongest.key(), strongest.label()),
                new PreAssessmentPillarRef(weakest.key(), weakest.label()), quote);
    }

    private static PreAssessmentReportPillar pillar(String key, String label, double rawScore) {
        return new PreAssessmentReportPillar(key, label, round1(rawScore));
    }

    private static double listBreadthScore(String[] values, String emptyNeedle) {
        if (values == null) {
            return 1;
        }
        long realValues = java.util.Arrays.stream(values)
                .filter(v -> v != null && !v.toLowerCase().contains(emptyNeedle))
                .count();
        return realValues == 0 ? 1 : clampScore(1 + realValues * 0.45);
    }

    private static double safeScore(Map<String, Double> map, String key) {
        return map.getOrDefault(key, 1.0);
    }

    private static double reverseFrequencyScore(String key) {
        return clampScore(5 - safeScore(FREQUENCY_SCORE, key));
    }

    private static double clampScore(double value) {
        return Math.max(1, Math.min(5, value));
    }

    private static double avgScore(double... values) {
        double sum = 0;
        for (double v : values) {
            sum += v;
        }
        return clampScore(sum / values.length);
    }

    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
