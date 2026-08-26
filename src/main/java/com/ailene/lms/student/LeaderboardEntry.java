package com.ailene.lms.student;

public record LeaderboardEntry(Integer rank, String accessId, String fullName, String avatar, Long totalXp,
        Boolean isMe) {
}
