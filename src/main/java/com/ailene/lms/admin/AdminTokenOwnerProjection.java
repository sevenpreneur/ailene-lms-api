package com.ailene.lms.admin;

import java.util.UUID;

public interface AdminTokenOwnerProjection {
    UUID getUserId();

    String getEmail();

    String getRole();

    String getStatus();

    Boolean getDeleted();
}
