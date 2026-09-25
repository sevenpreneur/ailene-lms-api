package com.ailene.lms.preassessment;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.chapter.ChapterRepository;
import com.ailene.lms.common.exception.BadRequestException;
import com.ailene.lms.common.qstash.QStashClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PreAssessmentRegenerateTest {

    private final AccessRepository accessRepository = mock(AccessRepository.class);
    private final PreAssessmentRepository preAssessmentRepository = mock(PreAssessmentRepository.class);
    private final PreAssessmentReportRepository reportRepository = mock(PreAssessmentReportRepository.class);
    private final QStashClient qStashClient = mock(QStashClient.class);
    private final PreAssessmentService service = new PreAssessmentService(accessRepository,
            mock(ChapterRepository.class), preAssessmentRepository, reportRepository,
            mock(PreAssessmentRecommendationGenerator.class), qStashClient);

    private final UUID userId = UUID.randomUUID();
    private final PreAssessmentReport report = new PreAssessmentReport();

    @BeforeEach
    void setUp() {
        Access access = new Access();
        access.setId("a1");
        PreAssessment preAssessment = new PreAssessment();
        preAssessment.setId(9);
        when(accessRepository.findByUserIdAndProjectId(userId, "p1")).thenReturn(Optional.of(access));
        when(preAssessmentRepository.findByAccessId("a1")).thenReturn(Optional.of(preAssessment));
        when(reportRepository.findByPreAssessmentId(9)).thenReturn(Optional.of(report));
        report.setPreAssessmentId(9);
    }

    @Test
    void regenerate_failedReport_requeuesAndResets() {
        report.setStatus(PreAssessmentReportStatus.failed);
        report.setErrorMessage("OpenAI request failed: 429 TOO_MANY_REQUESTS");
        report.setUpdatedAt(OffsetDateTime.now().minusMinutes(10));

        PreAssessmentRecommendationsResponse response = regenerate();

        assertThat(response.status()).isEqualTo(PreAssessmentReportStatus.pending);
        assertThat(response.errorMessage()).isNull();
        assertThat(report.getStatus()).isEqualTo(PreAssessmentReportStatus.pending);
        assertThat(report.getErrorMessage()).isNull();
        verify(reportRepository).save(report);
        verify(qStashClient).publish("/api/v1/pre-assessment/report-callback", new PreAssessmentReportJobRequest(9));
    }

    @Test
    void regenerate_failedJustNow_isRateLimited() {
        report.setStatus(PreAssessmentReportStatus.failed);
        report.setUpdatedAt(OffsetDateTime.now().minusSeconds(20));

        assertRejected("Tunggu sebentar sebelum mencoba lagi");
    }

    @Test
    void regenerate_stillGenerating_isRejectedUntilStuck() {
        report.setStatus(PreAssessmentReportStatus.processing);
        report.setUpdatedAt(OffsetDateTime.now().minusMinutes(2));
        assertRejected("Rekomendasi sedang dibuat, mohon tunggu sebentar");

        report.setUpdatedAt(OffsetDateTime.now().minusMinutes(6));
        assertThat(regenerate().status()).isEqualTo(PreAssessmentReportStatus.pending);
    }

    @Test
    void regenerate_completed_isRejected() {
        report.setStatus(PreAssessmentReportStatus.completed);
        report.setUpdatedAt(OffsetDateTime.now().minusDays(1));

        assertRejected("Rekomendasi sudah selesai dibuat");
    }

    private void assertRejected(String message) {
        assertThatThrownBy(this::regenerate).isInstanceOf(BadRequestException.class).hasMessage(message);
        verify(qStashClient, never()).publish(anyString(), any());
    }

    private PreAssessmentRecommendationsResponse regenerate() {
        return service.regenerateRecommendations(userId, new PreAssessmentProjectRequest("p1"));
    }
}
