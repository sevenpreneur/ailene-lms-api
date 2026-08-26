package com.ailene.lms.student;

import java.util.List;

public record LevelProgressResponse(Long totalXp, Short currentLevelNumber, String currentLevelName,
        Integer tasksRequired, Integer tasksDone, Boolean nextLevelUnlockable, Integer useCaseApprovedCount,
        Integer promptApprovedCount, Double hoursSavedTotal, List<String> toolsMastered) {
}
