package com.ailene.lms.access;

public interface ProjectAccessProjection {
    String getId();

    String getName();

    String getCompanyName();

    String getCompanySlug();

    String getAvatar();

    Integer getGroupId();

    String getGroupName();

    String getRole();

    Boolean getHasPreAssessment();
}
