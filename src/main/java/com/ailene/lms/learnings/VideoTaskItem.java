package com.ailene.lms.learnings;

import com.ailene.lms.video.VideoTaskProjection;
import com.ailene.lms.video.VideoTimestampItem;

import java.util.List;

public record VideoTaskItem(Integer id, String title, String description, String videoUrl, Short xpReward,
        Short orderIndex, Integer xpEarned, Boolean completed, List<VideoTimestampItem> timestamps) {

    public static VideoTaskItem from(VideoTaskProjection projection, List<VideoTimestampItem> timestamps) {
        return new VideoTaskItem(projection.getId(), projection.getTitle(), projection.getDescription(),
                projection.getVideoUrl(), projection.getXpReward(), projection.getOrderIndex(),
                projection.getXpEarned(), projection.getCompleted(), timestamps);
    }
}
