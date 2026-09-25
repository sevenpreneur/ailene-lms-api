package com.ailene.lms.admin;

import java.time.Instant;
import java.util.UUID;

public interface AdminMemberProjection {
    String getAccessId();

    String getRole();

    Integer getGroupId();

    String getGroupName();

    Instant getJoinedAt();

    UUID getUserId();

    String getFullName();

    String getEmail();

    String getAvatar();

    String getJobTitle();

    Instant getLastActiveAt();
}
