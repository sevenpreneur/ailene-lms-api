package com.ailene.lms.student;

import com.ailene.lms.chapter.ChapterListProjection;
import com.ailene.lms.chapter.ChapterMethod;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record ChapterListItem(Integer id, String name, String description, OffsetDateTime sessionDate,
        Integer durationMinutes, String locationName, String locationUrl, ChapterMethod method,
        ChapterLevelSummary level, Integer doneTasks, Integer totalTasks, ChapterProgress progress) {

    public static ChapterListItem from(ChapterListProjection projection) {
        int done = projection.getDoneTasks() == null ? 0 : projection.getDoneTasks().intValue();
        int total = projection.getTotalTasks() == null ? 0 : projection.getTotalTasks().intValue();

        ChapterProgress progress;
        if (total == 0 || done == 0) {
            progress = ChapterProgress.not_started;
        } else if (done >= total) {
            progress = ChapterProgress.completed;
        } else {
            progress = ChapterProgress.in_progress;
        }

        ChapterLevelSummary level = new ChapterLevelSummary(projection.getLevelId(), projection.getLevelNumber(),
                projection.getLevelName());

        return new ChapterListItem(projection.getId(), projection.getName(), projection.getDescription(),
                projection.getSessionDate().atOffset(ZoneOffset.UTC), projection.getDurationMinutes(),
                projection.getLocationName(), projection.getLocationUrl(),
                ChapterMethod.valueOf(projection.getMethod()), level, done, total, progress);
    }
}
