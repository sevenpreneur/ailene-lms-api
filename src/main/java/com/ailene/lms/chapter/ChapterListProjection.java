package com.ailene.lms.chapter;

import java.time.Instant;

public interface ChapterListProjection {
    Integer getId();

    String getName();

    String getDescription();

    Instant getSessionDate();

    Integer getDurationMinutes();

    String getLocationName();

    String getLocationUrl();

    String getMethod();

    Integer getLevelId();

    Short getLevelNumber();

    String getLevelName();

    Long getTotalTasks();

    Long getDoneTasks();
}
