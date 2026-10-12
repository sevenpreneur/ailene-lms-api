package com.ailene.lms.preassessment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PreAssessmentBandEngineTest {

    private static final int F1 = 1, DA1 = 2, R1 = 3, F2 = 4, T2 = 5, T4 = 6, C1 = 7, B1 = 8, B2 = 9, B6 = 10, A1 = 11;

    private static final List<PreAssessmentBandEngine.Item> ITEMS = List.of(
            item(A1, "profile", "single_choice", null, null, null, false, false, null, Map.of()),
            item(F1, "scenario", "single_choice", 1, "knowledge", 1, false, false, null, Map.of()),
            item(DA1, "scenario", "single_choice", 4, "knowledge", 1, true, false, null, Map.of()),
            item(R1, "scenario", "single_choice", 5, "skills", 1, true, false, null, Map.of()),
            item(F2, "scenario", "single_choice", 1, "knowledge", 2, false, false, null, Map.of()),
            item(T2, "scenario", "single_choice", 3, "skills", 2, false, false, null, Map.of()),
            item(T4, "scenario", "single_choice", 3, "skills", 3, false, false, null, Map.of()),
            item(C1, "practice", "text", 3, "skills", null, false, false, null, Map.of(1, 6, 2, 9)),
            item(B1, "behaviour", "single_choice", 5, "behaviours", null, false, true, R1, Map.of()),
            item(B2, "behaviour", "single_choice", 4, "behaviours", null, false, true, DA1, Map.of()),
            item(B6, "behaviour", "text", 2, "behaviours", null, false, false, null, Map.of()));

    private static final PreAssessmentBandEngine.Config CONFIG =
            new PreAssessmentBandEngine.Config(1, 3, 2, 50, 0.5, 0.75, 0.25);

    private static final List<PreAssessmentBandEngine.Band> BANDS = List.of(
            band(0, 0, null, null, null), band(1, 0, "40", null, null), band(2, 1, "70", null, null),
            band(3, 2, "70", "60", null), band(4, 3, "70", "70", "50"), band(5, 3, "70", "80", "75"));

    private static PreAssessmentBandEngine.Item item(int id, String section, String type, Integer domain,
            String dimension, Integer level, boolean critical, boolean gate, Integer overclaim,
            Map<Integer, Integer> thresholds) {
        return new PreAssessmentBandEngine.Item(id, section, type, domain, dimension, level, critical, gate, overclaim,
                thresholds);
    }

    private static PreAssessmentBandEngine.Band band(int band, int level, String p1, String p2, String p3) {
        return new PreAssessmentBandEngine.Band(band, level, p1 == null ? null : new BigDecimal(p1),
                p2 == null ? null : new BigDecimal(p2), p3 == null ? null : new BigDecimal(p3));
    }

    // Every item fully correct, C1 rubric 12/12, every behaviour at 1.0.
    private static Map<Integer, PreAssessmentBandEngine.Answer> perfect() {
        Map<Integer, PreAssessmentBandEngine.Answer> answers = new HashMap<>();
        for (int id : new int[] {F1, DA1, R1, F2, T2, T4, B1, B2, B6}) {
            answers.put(id, answer(id, "1", null, false));
        }
        answers.put(C1, answer(C1, "1", 12, false));
        answers.put(A1, answer(A1, null, null, false));
        return answers;
    }

    private static PreAssessmentBandEngine.Answer answer(int id, String credit, Integer rubric, boolean sensitive) {
        return new PreAssessmentBandEngine.Answer(id, credit == null ? null : new BigDecimal(credit), rubric, sensitive);
    }

    private static PreAssessmentBandEngine.Result score(Map<Integer, PreAssessmentBandEngine.Answer> answers) {
        return PreAssessmentBandEngine.score(ITEMS, answers, CONFIG, BANDS);
    }

    @Test
    void perfectAnswers_reachBandFiveLevelThree() {
        PreAssessmentBandEngine.Result result = score(perfect());

        assertThat(result.p1()).isEqualByComparingTo("100");
        assertThat(result.rawBand()).isEqualTo(5);
        assertThat(result.finalBand()).isEqualTo(5);
        assertThat(result.level()).isEqualTo(3);
        assertThat(result.securityGatePassed()).isTrue();
        assertThat(result.behaviourGatePassed()).isTrue();
        assertThat(result.overclaim()).isFalse();
    }

    @Test
    void wrongCriticalItem_capsAtBandOne_andFlagsOverclaimOfItsPair() {
        Map<Integer, PreAssessmentBandEngine.Answer> answers = perfect();
        answers.put(DA1, answer(DA1, "0", null, false));

        PreAssessmentBandEngine.Result result = score(answers);

        assertThat(result.p1()).isEqualByComparingTo("75");
        assertThat(result.rawBand()).isEqualTo(5);
        assertThat(result.finalBand()).isEqualTo(1);
        assertThat(result.level()).isZero();
        assertThat(result.securityGatePassed()).isFalse();
        assertThat(result.overclaim()).isTrue();
    }

    @Test
    void weakBehaviourGateItem_capsAtBandTwo() {
        Map<Integer, PreAssessmentBandEngine.Answer> answers = perfect();
        answers.put(B1, answer(B1, "0.25", null, false));

        PreAssessmentBandEngine.Result result = score(answers);

        assertThat(result.behaviours()).isEqualByComparingTo("81.25");
        assertThat(result.behaviourGatePassed()).isFalse();
        assertThat(result.rawBand()).isEqualTo(5);
        assertThat(result.finalBand()).isEqualTo(2);
        assertThat(result.level()).isEqualTo(1);
    }

    @Test
    void practiceRubricBetweenThresholds_passesOnlyTheLevelOneVirtualItem() {
        Map<Integer, PreAssessmentBandEngine.Answer> answers = perfect();
        answers.put(C1, answer(C1, "0.58", 7, false));

        PreAssessmentBandEngine.Result result = score(answers);

        assertThat(result.p1()).isEqualByComparingTo("100");
        assertThat(result.p2()).isEqualByComparingTo("66.67");
        assertThat(result.finalBand()).isEqualTo(3);
        assertThat(result.level()).isEqualTo(2);
        assertThat(result.domainScores())
                .filteredOn(d -> d.domainId() == 3)
                .singleElement()
                .satisfies(d -> {
                    assertThat(d.itemCount()).isEqualTo(4);
                    assertThat(d.score()).isEqualByComparingTo("75");
                });
    }

    @Test
    void sensitiveDataInPractice_failsSecurityGate() {
        Map<Integer, PreAssessmentBandEngine.Answer> answers = perfect();
        answers.put(C1, answer(C1, "1", 12, true));

        PreAssessmentBandEngine.Result result = score(answers);

        assertThat(result.securityGatePassed()).isFalse();
        assertThat(result.finalBand()).isEqualTo(1);
    }

    @Test
    void nothingCorrect_staysAtBandZero() {
        Map<Integer, PreAssessmentBandEngine.Answer> answers = new HashMap<>();
        ITEMS.forEach(i -> answers.put(i.id(), answer(i.id(), "0", 0, false)));

        PreAssessmentBandEngine.Result result = score(answers);

        assertThat(result.rawBand()).isZero();
        assertThat(result.finalBand()).isZero();
        assertThat(result.knowledge()).isEqualByComparingTo("0");
    }
}
