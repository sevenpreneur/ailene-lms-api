package com.ailene.lms.group;

import java.time.Instant;

public interface GroupListProjection {
    Integer getId();

    String getName();

    Long getMemberCount();

    Instant getCreatedAt();
}
