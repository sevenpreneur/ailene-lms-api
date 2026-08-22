package com.ailene.lms.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public record ApiResponse<T>(boolean success, int code, StatusName status, String message, T data) {

    public static <T> ResponseEntity<ApiResponse<T>> success(HttpStatus httpStatus, String message, T data) {
        return ResponseEntity.status(httpStatus)
                .body(new ApiResponse<>(true, httpStatus.value(), StatusName.fromCode(httpStatus.value()), message, data));
    }

    public static ResponseEntity<ApiResponse<Void>> error(HttpStatus httpStatus, String message) {
        return ResponseEntity.status(httpStatus)
                .body(new ApiResponse<>(false, httpStatus.value(), StatusName.fromCode(httpStatus.value()), message, null));
    }
}
