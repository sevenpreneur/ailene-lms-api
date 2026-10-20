package com.ailene.lms;

import com.ailene.lms.common.deepseek.DeepSeekClient;
import com.ailene.lms.usecase.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Explicit opt-in: one real provider request with synthetic data, no database. */
@EnabledIfSystemProperty(named = "guided.ai.live", matches = "true")
class GuidedWorkflowLiveTest {
    @Test
    void providerReturnsUsableWorkflowForSyntheticOfficeWork() {
        LmsApplication.loadDotenv();
        String key = System.getenv("DEEPSEEK_API_KEY");
        if (key == null) key = System.getProperty("DEEPSEEK_API_KEY");
        assertThat(key).as("DEEPSEEK_API_KEY must be configured").isNotBlank();
        ObjectMapper mapper = new ObjectMapper();
        GuidedWorkflowGenerator generator = new GuidedWorkflowGenerator(new DeepSeekClient(key), mapper, new GuidedJson(mapper));
        GuidedSession session = new GuidedSession();
        session.setDailyTask("Saya merekap laporan penjualan mingguan dari beberapa cabang.");
        session.setFrequency(UseCaseFrequency.weekly);
        session.setExampleText("Data simulasi: Cabang A target 50 juta, realisasi 45 juta. Cabang B target 40 juta, realisasi 42 juta.");
        session.setMinutesWithoutAi(BigDecimal.valueOf(90));
        session.setCurrentProcess("Kumpulkan data, cek angka, hitung selisih, dan tulis ringkasan.");
        session.setCurrentToolsJson("[\"Excel\",\"ChatGPT\"]");
        session.setGenerationsUsed(1);
        GuidedWorkflow result = generator.generate(session, null);
        assertThat(result.steps()).hasSizeBetween(3, 7).allMatch(s -> !s.isDone());
        assertThat(result.estimatedMinutesWithAi()).isPositive();
        assertThat(result.steps()).anyMatch(s -> !s.promptTemplate().isBlank());
    }
}
