package com.ailene.lms.access;

public interface LevelProgressProjection {
    String getAccessId();

    Long getXpCount();

    Integer getCurrentLevelId();

    Short getCurrentLevelNumber();

    String getCurrentLevelName();

    Long getTasksRequired();

    Long getTasksDone();
}
