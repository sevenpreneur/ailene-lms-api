package com.ailene.lms.learnings;

import com.ailene.lms.video.VideoTaskProjection;

public record VideoTaskItem(Integer id, String title, String description, String videoUrl, Short xpReward,
        Short orderIndex, Integer xpEarned, Boolean completed) {

    public static VideoTaskItem from(VideoTaskProjection projection) {
        return new VideoTaskItem(projection.getId(), projection.getTitle(), projection.getDescription(),
                projection.getVideoUrl(), projection.getXpReward(), projection.getOrderIndex(),
                projection.getXpEarned(), projection.getCompleted());
    }
}
