package com.ailene.lms.champion;

public interface GateProgressProjection {
    Long getRequired();

    Long getDone();

    Integer getNextLevelId();

    Short getNextLevelNumber();
}
