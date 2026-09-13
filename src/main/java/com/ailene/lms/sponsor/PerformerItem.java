package com.ailene.lms.sponsor;

public record PerformerItem(int rank, String accessId, String fullName, String avatar, String department,
        short levelNumber, String levelCode, String levelName, int xp, int useCaseCount, double hours,
        int composite) {
}
