package com.ailene.lms.common.response;

public enum StatusName {
    OK,
    CREATED,
    NO_CONTENT,
    BAD_REQUEST,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    METHOD_NOT_ALLOWED,
    CONFLICT,
    INTERNAL_SERVER_ERROR,
    BAD_GATEWAY,
    SERVICE_UNAVAILABLE;

    public static StatusName fromCode(int code) {
        return switch (code) {
            case 200 -> OK;
            case 201 -> CREATED;
            case 204 -> NO_CONTENT;
            case 400 -> BAD_REQUEST;
            case 401 -> UNAUTHORIZED;
            case 403 -> FORBIDDEN;
            case 404 -> NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 409 -> CONFLICT;
            case 500 -> INTERNAL_SERVER_ERROR;
            case 502 -> BAD_GATEWAY;
            case 503 -> SERVICE_UNAVAILABLE;
            default -> INTERNAL_SERVER_ERROR;
        };
    }
}
