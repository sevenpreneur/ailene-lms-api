package com.ailene.lms.user;

import java.util.UUID;

public record UserDto(UUID id, String fullName, String email, String avatar, UserRole role, String jobTitle) {

    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getFullName(), user.getEmail(), user.getAvatar(), user.getRole(),
                user.getJobTitle());
    }
}
