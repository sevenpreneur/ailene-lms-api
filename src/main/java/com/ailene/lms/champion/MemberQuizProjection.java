package com.ailene.lms.champion;

import java.time.Instant;

public interface MemberQuizProjection {
    Integer getId();

    String getQuizId();

    String getQuizName();

    Short getScore();

    Instant getSubmittedAt();
}
