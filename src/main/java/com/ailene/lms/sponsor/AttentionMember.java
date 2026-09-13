package com.ailene.lms.sponsor;

public record AttentionMember(String accessId, String fullName, String avatar, String jobTitle, short levelNumber,
        String levelName, int acceptedUseCases, Long inactiveDays, String status, boolean needsAttention, int score) {
}
