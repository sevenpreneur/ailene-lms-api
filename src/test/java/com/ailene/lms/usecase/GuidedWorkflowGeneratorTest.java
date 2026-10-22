package com.ailene.lms.usecase;

import com.ailene.lms.common.deepseek.DeepSeekClient;
import com.ailene.lms.common.exception.BadGatewayException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GuidedWorkflowGeneratorTest {
    private final DeepSeekClient client = mock(DeepSeekClient.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final GuidedWorkflowGenerator generator = new GuidedWorkflowGenerator(client, mapper, new GuidedJson(mapper));

    static String workflow() {
        return """
                {"summary":"Rekap laporan yang bisa dicek","estimated_minutes_with_ai":35,"risks":["Cek ulang angka"],
                 "steps":[
                  {"order":1,"title":"Samarkan data","instruction":"Hapus identitas pribadi.","suggested_tool":"Manual","prompt_template":"","estimated_minutes":10,"verification":"Tidak ada identitas asli."},
                  {"order":2,"title":"Buat ringkasan","instruction":"Tempel data tersamarkan.","suggested_tool":"ChatGPT","prompt_template":"Sebagai analis, ringkas [DATA] menjadi tabel dan tiga temuan. Jangan mengarang angka.","estimated_minutes":15,"verification":"Angka sesuai sumber."},
                  {"order":3,"title":"Cek hasil","instruction":"Bandingkan dengan sumber.","suggested_tool":"Excel","prompt_template":"","estimated_minutes":10,"verification":"Total cocok dengan sumber."}]}
                """;
    }

    @Test
    void validatesSchemaAndAssignsFreshIdsPerGeneration() {
        GuidedWorkflow first = generator.parse(workflow(), 1);
        GuidedWorkflow second = generator.parse(workflow(), 2);
        assertThat(first.steps()).hasSize(3);
        assertThat(first.steps().getFirst().id()).isEqualTo(11);
        assertThat(second.steps().getFirst().id()).isEqualTo(21);
        assertThat(first.steps()).allMatch(s -> !s.isDone() && s.doneAt() == null);
    }

    @Test
    void rejectsInvalidOrderAndWrongNumberTypes() {
        assertThatThrownBy(() -> generator.parse(workflow().replace("\"order\":2", "\"order\":1"), 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> generator.parse(workflow().replace("\"estimated_minutes\":15", "\"estimated_minutes\":\"15\""), 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void retriesInvalidJsonOnceAndDoesNotTrustModelCompletionFields() {
        when(client.createJsonCompletion(anyString(), anyString(), isNull(), anyDouble()))
                .thenReturn("invalid", workflow());
        GuidedSession session = new GuidedSession();
        session.setGenerationsUsed(1);
        session.setDailyTask("Rekap laporan mingguan dari beberapa cabang.");
        session.setMinutesWithoutAi(BigDecimal.valueOf(90));
        assertThat(generator.generate(session, null).steps()).hasSize(3);
        verify(client, times(2)).createJsonCompletion(anyString(), anyString(), isNull(), anyDouble());
    }

    @Test
    void regenerationIncludesPreviousWorkflowAlongsideFeedback() {
        when(client.createJsonCompletion(anyString(), anyString(), isNull(), anyDouble())).thenReturn(workflow());
        GuidedSession session = new GuidedSession();
        session.setGenerationsUsed(2);
        session.setStepsJson(new GuidedJson(mapper).write(generator.parse(workflow(), 1).steps()));
        generator.generate(session, "Perbaiki langkah kedua.");
        verify(client).createJsonCompletion(anyString(), argThat(input ->
                input.contains("previous_workflow") && input.contains("Buat ringkasan")
                        && input.contains("Perbaiki langkah kedua.")), isNull(), anyDouble());
    }

    @Test
    void returnsBadGatewayAfterTwoInvalidResults() {
        when(client.createJsonCompletion(anyString(), anyString(), isNull(), anyDouble())).thenReturn("{}");
        assertThatThrownBy(() -> generator.generate(new GuidedSession(), null)).isInstanceOf(BadGatewayException.class);
        verify(client, times(2)).createJsonCompletion(anyString(), anyString(), isNull(), anyDouble());
    }
}
