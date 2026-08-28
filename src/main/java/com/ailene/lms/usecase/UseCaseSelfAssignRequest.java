package com.ailene.lms.usecase;

import jakarta.validation.constraints.NotNull;

public record UseCaseSelfAssignRequest(@NotNull Integer useCaseId) {
}
