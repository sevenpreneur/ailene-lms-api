package com.ailene.lms.usecase;

import jakarta.validation.constraints.NotNull;

public record UseCaseDetailsRequest(@NotNull Integer id) {
}
