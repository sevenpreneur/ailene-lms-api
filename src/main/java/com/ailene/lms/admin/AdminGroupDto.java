package com.ailene.lms.admin;

import com.ailene.lms.common.TimeUtils;
import com.ailene.lms.group.GroupListProjection;

import java.time.OffsetDateTime;

public record AdminGroupDto(Integer id, String name, long memberCount, OffsetDateTime createdAt) {

    public static AdminGroupDto from(GroupListProjection p) {
        return new AdminGroupDto(p.getId(), p.getName(), p.getMemberCount(),
                TimeUtils.toOffsetDateTime(p.getCreatedAt()));
    }
}
