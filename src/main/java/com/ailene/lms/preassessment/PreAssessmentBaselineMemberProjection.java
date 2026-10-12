package com.ailene.lms.preassessment;

import java.time.Instant;

public interface PreAssessmentBaselineMemberProjection {

    String getAccessId();

    Integer getGroupId();

    String getGroupName();

    String getFullName();

    String getAvatar();

    String getStatus();

    Integer getBand();

    Instant getSubmittedAt();
}
