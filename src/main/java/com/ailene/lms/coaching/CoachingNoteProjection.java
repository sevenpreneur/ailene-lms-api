package com.ailene.lms.coaching;

import java.time.Instant;
import java.util.UUID;

public interface CoachingNoteProjection {
    String getText();

    Instant getCreatedAt();

    UUID getStudentId();

    String getStudentName();

    String getStudentAvatar();

    UUID getChampionId();

    String getChampionName();

    String getChampionAvatar();
}
