package com.ailene.lms.preassessment;

import java.math.BigDecimal;

public interface PreAssessmentAnswerProjection {

    Integer getItemId();

    String getTextValue();

    BigDecimal getCredit();

    Integer getRubricTotal();

    Boolean getHasSensitiveData();

    String getAiFeedback();

    Boolean getIsGraded();
}
