package com.ailene.lms.champion;

import java.time.Instant;

public record TeamMemberItem(String accessId, TeamMemberUser user, TeamLevelRef currentLevel, long totalXp,
        int progressPercent, int useCaseCount, Instant lastActiveAt, String status) {
}
