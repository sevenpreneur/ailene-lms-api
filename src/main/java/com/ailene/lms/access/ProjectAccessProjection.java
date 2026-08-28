package com.ailene.lms.access;

public interface ProjectAccessProjection {
    String getId();

    String getName();

    String getAvatar();

    Integer getGroupId();

    String getGroupName();

    String getRole();

    Boolean getHasPreAssessment();
}
