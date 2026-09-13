package com.ailene.lms.champion;

import java.time.Instant;
import java.util.UUID;

public interface TeamMemberProjection {
    String getAccessId();

    Integer getGroupId();

    String getGroupName();

    Integer getLevelId();

    Short getLevelNumber();

    String getLevelName();

    Instant getJoinedAt();

    UUID getUserId();

    String getFullName();

    String getEmail();

    String getAvatar();

    String getJobTitle();

    Instant getLastActiveAt();
}
