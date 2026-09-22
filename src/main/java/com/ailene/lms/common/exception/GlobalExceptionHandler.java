package com.ailene.lms.common.exception;

import com.ailene.lms.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.util.Arrays;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ApiResponse.error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException ex) {
        return ApiResponse.error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException ex) {
        return ApiResponse.error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException ex) {
        return ApiResponse.error(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleRouteNotFound(NoResourceFoundException ex) {
        return ApiResponse.error(HttpStatus.NOT_FOUND, "No such endpoint");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ApiResponse.error(HttpStatus.METHOD_NOT_ALLOWED, "Every endpoint is POST-only");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ApiResponse.error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ApiResponse.error(HttpStatus.BAD_REQUEST, describeUnreadableBody(ex));
    }

    // A rejected enum names the field and its accepted values -- a 500 here sent clients hunting the wrong side.
    private static String describeUnreadableBody(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException invalid) {
            String field = jsonPath(invalid);
            Class<?> target = invalid.getTargetType();
            if (target != null && target.isEnum()) {
                String accepted = Arrays.stream(target.getEnumConstants()).map(String::valueOf)
                        .collect(Collectors.joining(", "));
                return "%s: '%s' is not one of [%s]".formatted(field, invalid.getValue(), accepted);
            }
            return "%s: '%s' has the wrong type".formatted(field, invalid.getValue());
        }
        if (cause instanceof MismatchedInputException mismatched) {
            return "%s: wrong type or missing".formatted(jsonPath(mismatched));
        }
        return "Malformed JSON request body";
    }

    private static String jsonPath(JacksonException ex) {
        String path = ex.getPath().stream()
                .map(ref -> ref.getPropertyName() == null ? "[" + ref.getIndex() + "]" : ref.getPropertyName())
                .collect(Collectors.joining("."));
        return path.isEmpty() ? "request body" : path.replace(".[", "[");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    }
}
