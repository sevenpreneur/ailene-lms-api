package com.ailene.lms.chapter;

import java.time.OffsetDateTime;

public interface ChapterListProjection {
    Integer getId();

    String getName();

    String getDescription();

    OffsetDateTime getSessionDate();

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
