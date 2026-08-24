package com.ailene.lms.student;

import com.ailene.lms.access.StudentStatusProjection;

public record StudentStatusResponse(Long xpCount, Short currentLevelNumber, Boolean hasPreAssessment) {

    public static StudentStatusResponse from(StudentStatusProjection projection) {
        return new StudentStatusResponse(projection.getXpCount(), projection.getCurrentLevelNumber(),
                projection.getHasPreAssessment());
    }
}
