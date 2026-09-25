package com.ailene.lms.admin;

import com.ailene.lms.access.AccessRole;
import com.ailene.lms.auth.PasswordHasher;
import com.ailene.lms.common.NanoId;
import com.ailene.lms.common.exception.BadRequestException;
import com.ailene.lms.common.exception.ConflictException;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import com.ailene.lms.common.mailtrap.MailtrapClient;
import com.ailene.lms.user.User;
import com.ailene.lms.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AdminMemberService {

    private final AdminGuard adminGuard;
    private final AdminRepository adminRepository;
    private final AdminGroupService adminGroupService;
    private final UserRepository userRepository;
    private final MailtrapClient mailtrapClient;
    private final PasswordHasher passwordHasher;
    private final String lmsAppUrl;

    public AdminMemberService(AdminGuard adminGuard, AdminRepository adminRepository,
            AdminGroupService adminGroupService, UserRepository userRepository, MailtrapClient mailtrapClient,
            PasswordHasher passwordHasher,
            @Value("${lms.app-url:https://lms.ailene.id}") String lmsAppUrl) {
        this.adminGuard = adminGuard;
        this.adminRepository = adminRepository;
        this.adminGroupService = adminGroupService;
        this.userRepository = userRepository;
        this.mailtrapClient = mailtrapClient;
        this.passwordHasher = passwordHasher;
        this.lmsAppUrl = lmsAppUrl.replaceAll("/+$", "");
    }

    public List<AdminMemberDto> listMembers(String token, ListMembersRequest request) {
        adminGuard.requireAdminOnProject(token, request.projectId());
        if (request.groupId() != null) {
            adminGroupService.requireGroup(request.projectId(), request.groupId());
        }
        return adminRepository.findMembers(request.projectId(), request.groupId(), null).stream()
                .map(AdminMemberDto::from)
                .toList();
    }

    // Access is granted before the email goes out, so a mail failure reports email_sent false instead of undoing it.
    public InviteMemberResponse inviteMember(String token, InviteMemberRequest request) {
        adminGuard.requireAdminOnProject(token, request.projectId());
        adminGroupService.requireGroup(request.projectId(), request.groupId());
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user != null && adminRepository.accessExists(request.projectId(), user.getId())) {
            throw new ConflictException("This user already has access to this project");
        }
        boolean isNewUser = user == null;
        if (isNewUser) {
            String fullName = trimToNull(request.fullName());
            if (fullName == null) {
                throw new BadRequestException("full_name is required for a user who has never been invited before");
            }
            user = new User();
            user.setId(UUID.randomUUID());
            user.setEmail(email);
            user.setFullName(fullName);
            String jobTitle = trimToNull(request.jobTitle());
            user.setJobTitle(jobTitle == null ? "" : jobTitle);
        }
        // An existing person keeps their profile and any password they have; only a missing one gets filled in.
        boolean hasNoPassword = user.getPasswordHash() == null || user.getPasswordHash().isBlank();
        String passwordToSend = request.password() != null && hasNoPassword ? request.password() : null;
        if (passwordToSend != null) {
            user.setPasswordHash(passwordHasher.hash(passwordToSend));
        }
        if (isNewUser || passwordToSend != null) {
            user.setUpdatedAt(OffsetDateTime.now());
            userRepository.save(user);
        }

        String accessId = NanoId.generate();
        try {
            adminRepository.insertAccess(accessId, request.projectId(), user.getId(), request.groupId(),
                    request.role().name());
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("This user already has access to this project");
        }
        AdminMemberDto member = requireMember(request.projectId(), accessId);

        String accessUrl = lmsAppUrl + "/" + request.projectId() + "/" + request.role().name();
        String projectName = adminRepository.findProjectName(request.projectId()).orElse("Ailene");
        AdminInviteEmail invite = AdminInviteEmail.build(member.user().fullName(), member.user().email(),
                passwordToSend, projectName, request.role(), member.group().name(), accessUrl);
        boolean emailSent = mailtrapClient.send(member.user().email(), member.user().fullName(), invite.subject(),
                invite.text(), invite.html(), "LMS Invite");
        return new InviteMemberResponse(member, emailSent, passwordToSend != null, accessUrl);
    }

    public AdminMemberDto updateMember(String token, UpdateMemberRequest request) {
        adminGuard.requireAdminOnProject(token, request.projectId());
        String fullName = trimToNull(request.fullName());
        String jobTitle = request.jobTitle() == null ? null : request.jobTitle().trim();
        if (request.role() == null && request.groupId() == null && fullName == null && jobTitle == null) {
            throw new BadRequestException("Nothing to update");
        }

        AdminMemberProjection current = adminRepository.findMember(request.projectId(), request.accessId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        if (request.groupId() != null) {
            adminGroupService.requireGroup(request.projectId(), request.groupId());
        }

        if (request.role() != null || request.groupId() != null) {
            AccessRole role = request.role() != null ? request.role() : AccessRole.valueOf(current.getRole());
            Integer groupId = request.groupId() != null ? request.groupId() : current.getGroupId();
            adminRepository.updateAccess(request.projectId(), request.accessId(), role.name(), groupId);
        }

        if (fullName != null || jobTitle != null) {
            User user = userRepository.findById(current.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
            if (fullName != null) {
                user.setFullName(fullName);
            }
            if (jobTitle != null) {
                user.setJobTitle(jobTitle);
            }
            user.setUpdatedAt(OffsetDateTime.now());
            userRepository.save(user);
        }

        return requireMember(request.projectId(), request.accessId());
    }

    // Removing access never erases learning history: an access that anything else points at is refused instead.
    public AdminDeleteResponse deleteMember(String token, DeleteMemberRequest request) {
        adminGuard.requireAdminOnProject(token, request.projectId());
        adminRepository.findMember(request.projectId(), request.accessId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        try {
            adminRepository.deleteAccess(request.projectId(), request.accessId());
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(
                    "This member already has activity in this project (progress, submissions or notes) and cannot be removed");
        }
        return new AdminDeleteResponse(true);
    }

    private AdminMemberDto requireMember(String projectId, String accessId) {
        return adminRepository.findMember(projectId, accessId)
                .map(AdminMemberDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
