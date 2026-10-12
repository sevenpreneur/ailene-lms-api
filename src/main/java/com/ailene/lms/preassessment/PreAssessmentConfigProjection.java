package com.ailene.lms.preassessment;

import java.math.BigDecimal;

public interface PreAssessmentConfigProjection {

    Integer getId();

    Integer getSecurityGateMaxBand();

    Integer getBehaviourGateFromBand();

    Integer getBehaviourGateMaxBand();

    BigDecimal getBehaviourGateMinScore();

    BigDecimal getBehaviourGateMinCredit();

    BigDecimal getOverclaimMinCredit();

    BigDecimal getOverclaimCappedCredit();

    Integer getTargetBand();
}
