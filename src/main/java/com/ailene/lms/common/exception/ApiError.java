package com.ailene.lms.common.exception;

import java.time.OffsetDateTime;

public record ApiError(OffsetDateTime timestamp, int status, String error, String message, String path) {
}
