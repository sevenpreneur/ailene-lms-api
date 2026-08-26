package com.ailene.lms.student;

import java.util.List;

public record LeaderboardResponse(GroupSummary group, Integer myRank, Integer total,
        List<LeaderboardEntry> leaderboard) {
}
