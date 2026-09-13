package com.ailene.lms.champion;

import java.time.Instant;

public interface TeamLevelHistoryProjection {
    String getAccessId();

    Short getLevelNumber();

    Instant getReachedAt();
}
