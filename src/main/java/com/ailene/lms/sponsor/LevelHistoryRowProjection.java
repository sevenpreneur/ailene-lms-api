package com.ailene.lms.sponsor;

import java.time.Instant;

public interface LevelHistoryRowProjection {
    String getAccessId();

    Short getLevelNumber();

    Instant getReachedAt();
}
