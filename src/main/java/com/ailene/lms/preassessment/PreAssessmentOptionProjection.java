package com.ailene.lms.preassessment;

import java.math.BigDecimal;

public interface PreAssessmentOptionProjection {

    Integer getId();

    Integer getItemId();

    String getText();

    BigDecimal getCredit();

    Boolean getIsDontKnow();

    String getFlag();
}
