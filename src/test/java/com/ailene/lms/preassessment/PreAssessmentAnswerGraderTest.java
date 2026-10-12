package com.ailene.lms.preassessment;

import com.ailene.lms.common.deepseek.DeepSeekClient;
import com.ailene.lms.common.exception.BadGatewayException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class PreAssessmentAnswerGraderTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final PreAssessmentAnswerGrader grader = new PreAssessmentAnswerGrader(mock(DeepSeekClient.class),
            objectMapper);

    private static final Map<String, Object> PRACTICE = Map.of(
            "max_score", 4,
            "flags_sensitive_data", true,
            "criteria", List.of(
                    Map.of("key", "goal", "name", "Tujuan", "scale", List.of("a", "b", "c")),
                    Map.of("key", "format", "name", "Format", "scale", List.of("a", "b", "c"))));

    private static final Map<String, Object> EXAMPLE = Map.of(
            "score_map", List.of(0, 33, 67, 100),
            "scale", List.of("a", "b", "c", "d"));

    private static final List<PreAssessmentAnswerGrader.Task> TASKS = List.of(
            new PreAssessmentAnswerGrader.Task("C1", "Tulis prompt", PRACTICE, "prompt"),
            new PreAssessmentAnswerGrader.Task("B6", "Ceritakan", EXAMPLE, "cerita"));

    @Test
    void parse_validReply_turnsScoresIntoCredits() {
        Map<String, PreAssessmentAnswerGrader.Grade> grades = grader.parse("""
                {"C1": {"scores": {"goal": 2, "format": "1"}, "has_sensitive_data": true, "feedback": " Bagus. "},
                 "B6": {"score": 2, "feedback": "Spesifik."}}""", TASKS);

        PreAssessmentAnswerGrader.Grade practice = grades.get("C1");
        assertThat(practice.rubricTotal()).isEqualTo(3);
        assertThat(practice.credit()).isEqualByComparingTo("0.75");
        assertThat(practice.sensitiveData()).isTrue();
        assertThat(practice.feedback()).isEqualTo("Bagus.");
        assertThat(practice.rubricScores()).isEqualTo("{\"goal\":2,\"format\":1}");

        PreAssessmentAnswerGrader.Grade example = grades.get("B6");
        assertThat(example.rubricTotal()).isEqualTo(2);
        assertThat(example.credit()).isEqualByComparingTo("0.67");
        assertThat(example.sensitiveData()).isFalse();
    }

    @Test
    void parse_scoreOutOfRange_isRejected() {
        assertThatThrownBy(() -> grader.parse("""
                {"C1": {"scores": {"goal": 3, "format": 1}}, "B6": {"score": 1}}""", TASKS))
                .isInstanceOf(BadGatewayException.class);
    }

    @Test
    void parse_missingItem_isRejected() {
        assertThatThrownBy(() -> grader.parse("{\"C1\": {\"scores\": {\"goal\": 1, \"format\": 1}}}", TASKS))
                .isInstanceOf(BadGatewayException.class);
    }

    @Test
    void parse_notJson_isRejected() {
        assertThatThrownBy(() -> grader.parse("bukan json", TASKS)).isInstanceOf(BadGatewayException.class);
    }
}
