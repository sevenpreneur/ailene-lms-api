package com.ailene.lms.preassessment;

import java.math.BigDecimal;

public interface PreAssessmentBaselineClusterScoreProjection {

    String getAccessId();

    String getClusterCode();

    BigDecimal getScore();
}
