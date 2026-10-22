package com.ailene.lms.usecase;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.access.AccessRole;
import com.ailene.lms.common.exception.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class GuidedUseCaseStoreAccessTest {
    @Mock private GuidedSessionRepository sessions;
    @Mock private AccessRepository accesses;
    @Mock private GuidedJson json;
    @InjectMocks private GuidedUseCaseStore store;

    private final UUID userId = UUID.randomUUID();
    private static final String PROJECT_ID = "test-project";

    @Test
    void fourthGenerationIsAllowedAndStillGetsANewAuditVersion() {
        GuidedSession session = new GuidedSession();
        session.setId(7);
        session.setGenerationsUsed(3);
        session.setDailyTask("Saya merekap laporan penjualan dari seluruh cabang setiap minggu.");
        session.setFrequency(UseCaseFrequency.weekly);
        session.setExampleText("Cabang A: 40 unit, cabang B: 30 unit.");
        session.setSensitiveDataConfirmed(true);
        session.setMinutesWithoutAi(BigDecimal.valueOf(90));
        session.setCurrentProcess("Gabungkan data cabang, lalu periksa jumlahnya.");
        when(sessions.lockOwned(eq(7), eq(userId), anyList())).thenReturn(Optional.of(session));
        when(json.steps(session.getStepsJson())).thenReturn(List.of());

        var reservation = store.reserve(userId, new GuidedGenerateRequest(7, "Gunakan Copilot."));

        assertThat(reservation.session().getGenerationsUsed()).isEqualTo(4);
        assertThat(reservation.token()).isNotBlank();
        verify(sessions).saveAndFlush(session);
    }

    @Test
    void championCanOpenOwnStudentModeDraftLookup() {
        Access access = access(AccessRole.champion);
        when(accesses.findByUserIdAndProjectId(userId, PROJECT_ID)).thenReturn(Optional.of(access));
        when(sessions.findFirstByStudentAccessIdAndProjectIdAndUseCaseIdIsNullAndStatusNotOrderByIdDesc(
                access.getId(), PROJECT_ID, GuidedStatus.submitted)).thenReturn(Optional.empty());

        assertThat(store.details(userId, new GuidedDetailsRequest(PROJECT_ID, null, null))).isNull();
        verify(sessions).findFirstByStudentAccessIdAndProjectIdAndUseCaseIdIsNullAndStatusNotOrderByIdDesc(
                access.getId(), PROJECT_ID, GuidedStatus.submitted);
    }

    @Test
    void sponsorCannotOpenStudentModeDraftLookup() {
        when(accesses.findByUserIdAndProjectId(userId, PROJECT_ID))
                .thenReturn(Optional.of(access(AccessRole.sponsor)));

        assertThatThrownBy(() -> store.details(userId, new GuidedDetailsRequest(PROJECT_ID, null, null)))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(sessions);
    }

    private static Access access(AccessRole role) {
        Access access = new Access();
        access.setId("test-access");
        access.setRole(role);
        return access;
    }
}
