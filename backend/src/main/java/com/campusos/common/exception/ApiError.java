package com.campusos.common.exception;

import java.time.Instant;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;

public record ApiError(@Schema(example = "2026-10-15T18:48:58Z") Instant timestamp, @Schema(example = "400") int status, @Schema(example = "VALIDATION_ERROR", description = "VALIDATION_ERROR, UNAUTHENTICATED, FORBIDDEN, NOT_FOUND, BOOKING_CONFLICT, ILLEGAL_TRANSITION, DUPLICATE_EMAIL, BUSINESS_RULE, or INTERNAL_ERROR") String code, @Schema(example = "The request contains invalid fields.") String message, @Schema(example = "/api/v1/issues") String path,
                       Map<String, String> fieldErrors) {
    public static ApiError of(int status, String code, String message, String path) {
        return new ApiError(Instant.now(), status, code, message, path, null);
    }

    public static ApiError withFields(int status, String code, String message, String path,
                                      Map<String, String> fieldErrors) {
        return new ApiError(Instant.now(), status, code, message, path, fieldErrors);
    }
}
