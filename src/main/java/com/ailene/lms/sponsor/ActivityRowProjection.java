package com.ailene.lms.sponsor;

import java.time.Instant;

public interface ActivityRowProjection {
    Instant getAt();

    String getActor();

    String getGroupName();

    String getSubject();

    Boolean getAccepted();
}
