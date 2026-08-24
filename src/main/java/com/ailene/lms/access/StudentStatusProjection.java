package com.ailene.lms.access;

public interface StudentStatusProjection {
    Long getXpCount();

    Short getCurrentLevelNumber();

    Boolean getHasPreAssessment();
}
