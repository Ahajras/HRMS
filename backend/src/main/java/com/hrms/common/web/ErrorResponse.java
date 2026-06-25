package com.hrms.common.web;

import java.time.Instant;
import java.util.List;

/**
 * Standard error body returned by the global exception handler.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {
    }

    public static ErrorResponse of(int status, String error, String code, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, code, message, path, List.of());
    }

    public static ErrorResponse of(int status, String error, String code, String message, String path,
                                   List<FieldError> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, error, code, message, path, fieldErrors);
    }
}
