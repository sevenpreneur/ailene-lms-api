package com.ailene.lms.champion;

import java.time.Instant;

public interface MemberNoteProjection {
    Integer getId();

    String getText();

    Instant getCreatedAt();

    String getChampionName();
}
