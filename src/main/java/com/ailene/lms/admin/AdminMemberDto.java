package com.ailene.lms.admin;

import com.ailene.lms.access.AccessRole;
import com.ailene.lms.common.TimeUtils;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminMemberDto(String accessId, AccessRole role, Group group, User user, OffsetDateTime joinedAt) {

    public record Group(Integer id, String name) {
    }

    public record User(UUID id, String fullName, String email, String avatar, String jobTitle,
            OffsetDateTime lastActiveAt) {
    }

    public static AdminMemberDto from(AdminMemberProjection p) {
        return new AdminMemberDto(p.getAccessId(), AccessRole.valueOf(p.getRole()),
                new Group(p.getGroupId(), p.getGroupName()),
                new User(p.getUserId(), p.getFullName(), p.getEmail(), p.getAvatar(), p.getJobTitle(),
                        TimeUtils.toOffsetDateTime(p.getLastActiveAt())),
                TimeUtils.toOffsetDateTime(p.getJoinedAt()));
    }
}
