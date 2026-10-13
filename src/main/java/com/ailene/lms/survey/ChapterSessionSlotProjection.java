package com.ailene.lms.survey;

import java.time.Instant;

public interface ChapterSessionSlotProjection {
    Integer getId();

    Instant getSessionDate();
}
