package com.ailene.lms.video;

public record VideoTimestampItem(Integer startSeconds, String title) {

    public static VideoTimestampItem from(VideoTimestampProjection projection) {
        return new VideoTimestampItem(projection.getStartSeconds(), projection.getTitle());
    }
}
