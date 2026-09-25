package com.ailene.lms.admin;

import com.ailene.lms.common.exception.ForbiddenException;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import com.ailene.lms.common.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminGuardTest {

    private final AdminRepository adminRepository = mock(AdminRepository.class);
    private final AdminGuard guard = new AdminGuard(adminRepository, new ObjectMapper());

    @Test
    void requireAdmin_activeAdministratorToken_passes() {
        String token = token(Instant.now().plusSeconds(3600));
        AdminTokenOwnerProjection owner = owner("administrator", "active", false);
        when(adminRepository.findActiveTokenOwner(token)).thenReturn(Optional.of(owner));

        assertThat(guard.requireAdmin(token)).isSameAs(owner);
    }

    @Test
    void requireAdmin_unknownOrInactiveToken_isUnauthorized() {
        String token = token(Instant.now().plusSeconds(3600));
        when(adminRepository.findActiveTokenOwner(token)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.requireAdmin(token)).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void requireAdmin_expiredToken_isUnauthorized() {
        String token = token(Instant.now().minusSeconds(60));
        AdminTokenOwnerProjection owner = owner("administrator", "active", false);
        when(adminRepository.findActiveTokenOwner(token)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> guard.requireAdmin(token)).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void requireAdmin_memberOrSuspendedOrDeleted_isForbidden() {
        String token = token(Instant.now().plusSeconds(3600));
        for (AdminTokenOwnerProjection owner : new AdminTokenOwnerProjection[] { owner("member", "active", false),
                owner("administrator", "suspended", false), owner("administrator", "active", true) }) {
            when(adminRepository.findActiveTokenOwner(token)).thenReturn(Optional.of(owner));

            assertThatThrownBy(() -> guard.requireAdmin(token)).isInstanceOf(ForbiddenException.class);
        }
    }

    @Test
    void requireAdminOnProject_unknownProject_isNotFound() {
        String token = token(Instant.now().plusSeconds(3600));
        AdminTokenOwnerProjection owner = owner("administrator", "active", false);
        when(adminRepository.findActiveTokenOwner(token)).thenReturn(Optional.of(owner));
        when(adminRepository.projectExists("nope")).thenReturn(false);

        assertThatThrownBy(() -> guard.requireAdminOnProject(token, "nope"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static String token(Instant exp) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String payload = "{\"sub\":\"" + UUID.randomUUID() + "\",\"exp\":" + exp.getEpochSecond() + "}";
        return encoder.encodeToString("{\"alg\":\"HS256\"}".getBytes(StandardCharsets.UTF_8)) + "."
                + encoder.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + ".sig";
    }

    private static AdminTokenOwnerProjection owner(String role, String status, boolean deleted) {
        AdminTokenOwnerProjection owner = mock(AdminTokenOwnerProjection.class);
        when(owner.getRole()).thenReturn(role);
        when(owner.getStatus()).thenReturn(status);
        when(owner.getDeleted()).thenReturn(deleted);
        return owner;
    }
}
