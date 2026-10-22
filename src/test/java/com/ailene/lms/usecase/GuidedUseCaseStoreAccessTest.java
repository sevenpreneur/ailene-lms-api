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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuidedUseCaseStoreAccessTest {
    @Mock private GuidedSessionRepository sessions;
    @Mock private AccessRepository accesses;
    @InjectMocks private GuidedUseCaseStore store;

    private final UUID userId = UUID.randomUUID();
    private static final String PROJECT_ID = "test-project";

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
