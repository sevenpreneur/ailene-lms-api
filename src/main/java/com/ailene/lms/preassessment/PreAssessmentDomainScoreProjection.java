package com.ailene.lms.preassessment;

import java.math.BigDecimal;

public interface PreAssessmentDomainScoreProjection {

    String getDomainCode();

    String getDomainName();

    String getClusterCode();

    String getClusterName();

    BigDecimal getScore();

    Integer getItemCount();
}
