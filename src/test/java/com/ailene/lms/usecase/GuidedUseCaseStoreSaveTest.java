package com.ailene.lms.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuidedUseCaseStoreSaveTest {
    @Mock private GuidedSessionRepository sessions;
    @Spy private GuidedJson json = new GuidedJson(new ObjectMapper());
    @InjectMocks private GuidedUseCaseStore store;

    private final UUID userId = UUID.randomUUID();
    private GuidedSession session;

    @BeforeEach
    void setUp() {
        session = new GuidedSession();
        session.setId(7);
        session.setProjectId("test-project");
        session.setDailyTask("Saya merekap laporan penjualan mingguan dari beberapa cabang.");
        session.setFrequency(UseCaseFrequency.weekly);
        session.setExampleText("Cabang A: 40 unit, cabang B: 30 unit.");
        session.setSensitiveDataConfirmed(true);
        session.setMinutesWithoutAi(new BigDecimal("90.00"));
        session.setCurrentProcess("Gabungkan data cabang, lalu periksa jumlahnya.");
        session.setCurrentToolsJson("[\"Excel\", \"Email\"]");
        session.setStepsJson(json.write(new GuidedWorkflowGenerator(null, new ObjectMapper(), json)
                .parse(GuidedWorkflowGeneratorTest.workflow(), 1).steps()));
        session.setCurrentStep(4);
        session.setStatus(GuidedStatus.steps_ready);
        when(sessions.lockOwned(eq(7), eq(userId), anyList())).thenReturn(Optional.of(session));
    }

    @Test
    void acceptingWorkflowDoesNotInvalidateEquivalentDecimalAndJsonFormatting() {
        String workflow = session.getStepsJson();
        GuidedSaveRequest request = new GuidedSaveRequest(null, null, 7, 5,
                session.getDailyTask(), session.getFrequency(), session.getExampleText(), "", true,
                new BigDecimal("90"), session.getCurrentProcess(), List.of("Excel", "Email"), "");

        GuidedSessionResponse result = store.save(userId, request);

        assertThat(result.currentStep()).isEqualTo(5);
        assertThat(result.status()).isEqualTo(GuidedStatus.in_progress);
        assertThat(result.steps()).hasSize(3);
        assertThat(session.getStepsJson()).isEqualTo(workflow);
    }

    @Test
    void navigationOnlySaveKeepsTheGeneratedWorkflow() {
        GuidedSaveRequest request = new GuidedSaveRequest(null, null, 7, 5,
                null, null, null, null, null, null, null, null, null);

        GuidedSessionResponse result = store.save(userId, request);

        assertThat(result.currentStep()).isEqualTo(5);
        assertThat(result.steps()).hasSize(3);
    }

    @Test
    void aRealTaskChangeStillInvalidatesTheWorkflow() {
        GuidedSaveRequest request = new GuidedSaveRequest(null, null, 7, 2,
                "Saya membuat ringkasan keluhan pelanggan untuk tindak lanjut tim.",
                null, null, null, null, null, null, null, null);

        GuidedSessionResponse result = store.save(userId, request);

        assertThat(result.steps()).isEmpty();
        assertThat(result.status()).isEqualTo(GuidedStatus.intake);
    }
}
