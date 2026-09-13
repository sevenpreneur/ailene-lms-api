package com.ailene.lms.sponsor;

import java.time.Instant;

public interface XpRowProjection {
    String getAccessId();

    Integer getXpEarned();

    Instant getEarnedAt();
}
