package com.ailene.lms.material;

public interface MaterialTaskProjection {
    String getId();

    String getTitle();

    String getDescription();

    Short getXpReward();

    Short getOrderIndex();

    Boolean getCompleted();
}
