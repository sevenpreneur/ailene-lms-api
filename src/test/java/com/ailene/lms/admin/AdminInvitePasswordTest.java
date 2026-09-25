package com.ailene.lms.admin;

import com.ailene.lms.access.AccessRole;
import com.ailene.lms.auth.PasswordHasher;
import com.ailene.lms.common.mailtrap.MailtrapClient;
import com.ailene.lms.group.ProjectGroup;
import com.ailene.lms.user.User;
import com.ailene.lms.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminInvitePasswordTest {

    private final AdminRepository adminRepository = mock(AdminRepository.class);
    private final AdminGroupService groupService = mock(AdminGroupService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final MailtrapClient mailtrapClient = mock(MailtrapClient.class);
    private final PasswordHasher hasher = new PasswordHasher();
    private final AdminMemberService service = new AdminMemberService(mock(AdminGuard.class), adminRepository,
            groupService, userRepository, mailtrapClient, hasher, "https://lms.ailene.id");

    @BeforeEach
    void setUp() {
        when(groupService.requireGroup("p1", 1)).thenReturn(new ProjectGroup());
        when(adminRepository.findProjectName("p1")).thenReturn(Optional.of("Program"));
        when(mailtrapClient.send(anyString(), any(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true);
    }

    @Test
    void invite_newPerson_setsPasswordAndEmailsIt() {
        when(userRepository.findByEmailIgnoreCase("rani@example.com")).thenReturn(Optional.empty());
        stubMember();

        InviteMemberResponse response = service.inviteMember("tok", request("Rahasia#2026"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(hasher.matches("Rahasia#2026", saved.getValue().getPasswordHash())).isTrue();
        assertThat(response.passwordSet()).isTrue();
        verify(mailtrapClient).send(eq("rani@example.com"), any(),
                anyString(), contains("Password: Rahasia#2026"), anyString(), anyString());
    }

    @Test
    void invite_existingPerson_keepsTheirPasswordAndLeavesItOutOfTheEmail() {
        User existing = new User();
        existing.setId(UUID.randomUUID());
        existing.setEmail("rani@example.com");
        existing.setPasswordHash(hasher.hash("password-lama-rani"));
        String originalHash = existing.getPasswordHash();
        when(userRepository.findByEmailIgnoreCase("rani@example.com")).thenReturn(Optional.of(existing));
        when(adminRepository.accessExists("p1", existing.getId())).thenReturn(false);
        stubMember();

        InviteMemberResponse response = service.inviteMember("tok", request("Rahasia#2026"));

        assertThat(existing.getPasswordHash()).isEqualTo(originalHash);
        verify(userRepository, never()).save(any());
        assertThat(response.passwordSet()).isFalse();
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(mailtrapClient).send(eq("rani@example.com"), any(), anyString(), text.capture(), anyString(),
                anyString());
        assertThat(text.getValue()).doesNotContain("Rahasia#2026").doesNotContain("Password:");
    }

    @Test
    void invite_existingPersonWithoutPassword_getsTheAdminsPassword() {
        User existing = new User();
        existing.setId(UUID.randomUUID());
        existing.setEmail("rani@example.com");
        existing.setFullName("Rani Lama");
        when(userRepository.findByEmailIgnoreCase("rani@example.com")).thenReturn(Optional.of(existing));
        when(adminRepository.accessExists("p1", existing.getId())).thenReturn(false);
        stubMember();

        InviteMemberResponse response = service.inviteMember("tok", request("Rahasia#2026"));

        assertThat(hasher.matches("Rahasia#2026", existing.getPasswordHash())).isTrue();
        assertThat(existing.getFullName()).isEqualTo("Rani Lama");
        assertThat(response.passwordSet()).isTrue();
        verify(userRepository).save(existing);
        verify(mailtrapClient).send(eq("rani@example.com"), any(), anyString(), contains("Password: Rahasia#2026"),
                anyString(), anyString());
    }

    private InviteMemberRequest request(String password) {
        return new InviteMemberRequest("p1", "rani@example.com", "Rani", "HR", AccessRole.student, 1, password);
    }

    private void stubMember() {
        AdminMemberProjection row = mock(AdminMemberProjection.class);
        when(row.getRole()).thenReturn("student");
        when(row.getEmail()).thenReturn("rani@example.com");
        when(row.getFullName()).thenReturn("Rani");
        when(row.getGroupName()).thenReturn("Finance");
        when(row.getJoinedAt()).thenReturn(Instant.now());
        when(adminRepository.findMember(eq("p1"), anyString())).thenReturn(Optional.of(row));
    }
}
