package com.ailene.lms.student;

import java.util.List;

public record CompetencyProfileResponse(List<CompetencyDimension> dimensions, Double avg, Short currentLevelNumber,
        Integer tierNumber, String tierName, NextTier nextTier) {
}
