package com.ailene.lms.coaching;

import com.ailene.lms.common.AssignedByUser;
import com.ailene.lms.common.TimeUtils;

import java.time.OffsetDateTime;

public record CoachingNoteItem(AssignedByUser student, AssignedByUser champion, String text,
        OffsetDateTime createdAt) {

    public static CoachingNoteItem from(CoachingNoteProjection projection) {
        AssignedByUser student = new AssignedByUser(projection.getStudentId(), projection.getStudentName(),
                projection.getStudentAvatar());
        AssignedByUser champion = new AssignedByUser(projection.getChampionId(), projection.getChampionName(),
                projection.getChampionAvatar());
        return new CoachingNoteItem(student, champion, projection.getText(),
                TimeUtils.toOffsetDateTime(projection.getCreatedAt()));
    }
}
