package com.ailene.lms.learnings;

import com.ailene.lms.material.MaterialTaskProjection;

public record MaterialTaskItem(String id, String title, String description, Short xpReward, Short orderIndex,
        Boolean completed) {

    public static MaterialTaskItem from(MaterialTaskProjection projection) {
        return new MaterialTaskItem(projection.getId(), projection.getTitle(), projection.getDescription(),
                projection.getXpReward(), projection.getOrderIndex(), projection.getCompleted());
    }
}
