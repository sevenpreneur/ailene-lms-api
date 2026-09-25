package com.ailene.lms.preassessment;

import com.ailene.lms.common.deepseek.DeepSeekClient;
import com.ailene.lms.common.exception.BadGatewayException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class PreAssessmentRecommendationGeneratorTest {

    private final PreAssessmentRecommendationGenerator generator = new PreAssessmentRecommendationGenerator(
            mock(DeepSeekClient.class), new ObjectMapper());
    private final List<String> lessons = List.of("Prompting Dasar", "Analisis Data dengan AI");

    @Test
    @SuppressWarnings("unchecked")
    void parse_rebuildsTheBlobTheFrontendReads() {
        Map<String, Object> result = generator.parse("""
                {"time_saved_label":"±6 jam/minggu","extra":"x","items":[
                 {"source":"Laporan","title":"Draf laporan","impact":"tinggi","speed":"±2 jam","description":"d",
                  "lessons":["prompting dasar","Chapter Karangan","Prompting Dasar"],"extra":1},
                 {"title":"Tanpa deskripsi"},
                 {"title":"Rekap","impact":"Sangat tinggi","description":"d2"}
                ]}""", lessons);

        assertThat(result).containsOnlyKeys("time_saved_label", "items");
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertThat(items).hasSize(2);
        assertThat(items.get(0)).containsOnlyKeys("source", "title", "impact", "speed", "description", "lessons")
                .containsEntry("impact", "Tinggi")
                .containsEntry("lessons", List.of("Prompting Dasar"));
        assertThat(items.get(1)).containsEntry("impact", "Sedang").containsEntry("source", "-")
                .containsEntry("lessons", List.of());
    }

    @Test
    void parse_noUsableItem_isBadGateway() {
        assertThatThrownBy(() -> generator.parse("{\"items\":[{\"title\":\"x\"}]}", lessons))
                .isInstanceOf(BadGatewayException.class);
        assertThatThrownBy(() -> generator.parse("not json", lessons)).isInstanceOf(BadGatewayException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void parse_capsAtFiveItems() {
        StringBuilder items = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            items.append(i > 0 ? "," : "").append("{\"title\":\"t").append(i).append("\",\"description\":\"d\"}");
        }

        Map<String, Object> result = generator.parse("{\"items\":[" + items + "]}", lessons);

        assertThat((List<Object>) result.get("items")).hasSize(5);
        assertThat(result).containsEntry("time_saved_label", "-");
    }
}
