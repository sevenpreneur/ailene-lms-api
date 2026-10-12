package com.ailene.lms.preassessment;

import java.math.BigDecimal;

public interface PreAssessmentBandProjection {

    Integer getBand();

    String getName();

    Integer getLevel();

    BigDecimal getMinP1();

    BigDecimal getMinP2();

    BigDecimal getMinP3();
}
