package com.ailene.lms.material;

import java.time.Instant;

public interface LevelMaterialProjection {
    String getMaterialId();

    String getMaterialTitle();

    Short getOrderIndex();

    Instant getSessionDate();

    Boolean getCompleted();
}
