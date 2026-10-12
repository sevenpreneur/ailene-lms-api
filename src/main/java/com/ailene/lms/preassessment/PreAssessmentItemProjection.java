package com.ailene.lms.preassessment;

public interface PreAssessmentItemProjection {

    Integer getId();

    String getItemCode();

    String getSection();

    String getType();

    String getQuestion();

    Integer getDomainId();

    String getDimension();

    Integer getCfteLevel();

    Boolean getIsCritical();

    Boolean getIsBehaviourGate();

    Integer getOverclaimItemId();

    String getGradingConfig();
}
