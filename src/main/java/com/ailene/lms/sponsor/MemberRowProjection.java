package com.ailene.lms.sponsor;

import java.time.Instant;
import java.util.UUID;

public interface MemberRowProjection {
    String getAccessId();

    String getRole();

    Integer getGroupId();

    String getGroupName();

    Integer getLevelId();

    Short getLevelNumber();

    String getLevelName();

    Instant getCreatedAt();

    UUID getUserId();

    String getFullName();

    String getEmail();

    String getAvatar();

    String getJobTitle();

    Instant getLastActiveAt();
}
