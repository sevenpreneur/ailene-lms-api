package com.ailene.lms.role;

import java.time.OffsetDateTime;

public record RoleDto(Short id, String name, Short permission, OffsetDateTime createdAt, OffsetDateTime updatedAt) {

    static RoleDto from(Role role) {
        return new RoleDto(role.getId(), role.getName(), role.getPermission(), role.getCreatedAt(), role.getUpdatedAt());
    }
}
