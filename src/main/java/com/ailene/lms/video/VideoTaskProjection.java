package com.ailene.lms.video;

public interface VideoTaskProjection {
    Integer getId();

    String getTitle();

    String getDescription();

    String getVideoUrl();

    Short getXpReward();

    Short getOrderIndex();

    Integer getXpEarned();

    Boolean getCompleted();
}
