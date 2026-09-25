package com.ailene.lms.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordLoginRequest(@NotBlank @Size(max = 255) String email, @NotBlank @Size(max = 72) String password) {
}
