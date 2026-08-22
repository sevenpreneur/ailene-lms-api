package com.ailene.lms.response;

public enum StatusName {
    OK,
    CREATED,
    NO_CONTENT,
    BAD_REQUEST,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    INTERNAL_SERVER_ERROR;

    public static StatusName fromCode(int code) {
        return switch (code) {
            case 200 -> OK;
            case 201 -> CREATED;
            case 204 -> NO_CONTENT;
            case 400 -> BAD_REQUEST;
            case 401 -> UNAUTHORIZED;
            case 403 -> FORBIDDEN;
            case 404 -> NOT_FOUND;
            case 409 -> CONFLICT;
            case 500 -> INTERNAL_SERVER_ERROR;
            default -> INTERNAL_SERVER_ERROR;
        };
    }
}
