package com.ailene.lms.dto;

import com.ailene.lms.entity.Role;

import java.time.OffsetDateTime;

public record RoleDto(Short id, String name, Short permission, OffsetDateTime createdAt, OffsetDateTime updatedAt) {

    public static RoleDto from(Role role) {
        return new RoleDto(role.getId(), role.getName(), role.getPermission(), role.getCreatedAt(),
                role.getUpdatedAt());
    }
}
