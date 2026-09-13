package com.ailene.lms.champion;

import com.ailene.lms.sponsor.DepartmentRef;

import java.time.Instant;

public record MemberDetailInfo(String accessId, String fullName, String email, String avatar, String jobTitle,
        DepartmentRef group, TeamLevelRef currentLevel, Instant joinedAt, Instant lastActiveAt) {
}
