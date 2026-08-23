package com.ailene.lms.auth;

import com.ailene.lms.access.ProjectAccessDto;
import com.ailene.lms.user.UserDto;

import java.util.List;

public record CheckSessionResponse(UserDto user, List<ProjectAccessDto> projectAccess) {
}
