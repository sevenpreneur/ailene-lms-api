package com.ailene.lms.usecase;

import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.common.exception.BadRequestException;
import com.ailene.lms.level.LevelRepository;
import com.ailene.lms.notification.NotificationWriter;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GuidedPrefillTest {
    private final GuidedSessionRepository sessions = mock(GuidedSessionRepository.class);
    private final GuidedJson json = new GuidedJson(new ObjectMapper());
    private final GuidedUseCaseStore store = new GuidedUseCaseStore(sessions, mock(GuidedGenerationRepository.class),
            mock(UseCaseRepository.class), mock(UseCaseSubmissionRepository.class), mock(AccessRepository.class),
            mock(LevelRepository.class), mock(NotificationWriter.class), json, mock(EntityManager.class));

    @Test
    void prefillOpensOnWorkflowStepWithChampionIntake() {
        store.prefill("s1", "p1", 7, prefill(" Saya merekap laporan penjualan mingguan dari tiga cabang. ", "Cabang A: 45 juta"));

        ArgumentCaptor<GuidedSession> saved = ArgumentCaptor.forClass(GuidedSession.class);
        verify(sessions).save(saved.capture());
        GuidedSession s = saved.getValue();
        assertThat(s.getStudentAccessId()).isEqualTo("s1");
        assertThat(s.getUseCaseId()).isEqualTo(7);
        assertThat(s.getCurrentStep()).isEqualTo(4);
        assertThat(s.getStatus()).isEqualTo(GuidedStatus.intake);
        assertThat(s.getDailyTask()).startsWith("Saya");
        assertThat(s.isSensitiveDataConfirmed()).isTrue();
        assertThat(json.strings(s.getCurrentToolsJson())).containsExactly("Excel", "Email");
        assertThat(s.getGenerationsUsed()).isZero();
    }

    @Test
    void requirePrefillRejectsSensitiveDataWithoutSaving() {
        assertThatThrownBy(() -> store.requirePrefill(
                prefill("Saya merekap laporan penjualan mingguan dari tiga cabang.", "NIK 1234567890123456")))
                .isInstanceOf(BadRequestException.class);
        verify(sessions, never()).save(any());
    }

    private static GuidedPrefill prefill(String task, String example) {
        return new GuidedPrefill(task, UseCaseFrequency.weekly, example, null, true, new BigDecimal("90"),
                "Kumpulkan data, cek angka, lalu tulis ringkasan.", List.of(" Excel", "Email", "Excel"), null);
    }
}
