package com.ailene.lms.auth;

import com.ailene.lms.user.UserDto;

public record AuthLoginResponse(String token, UserDto user) {
}
