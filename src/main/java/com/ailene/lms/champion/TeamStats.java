package com.ailene.lms.champion;

public record TeamStats(int total, int onTrack, int atRisk, int behind, int activeThisWeek, int submissionsSent,
        int membersSubmitted, long hoursSaved) {
}
