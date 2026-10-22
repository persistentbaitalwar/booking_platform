package com.persistent.booking_platform.exception;

import java.time.Instant;
import java.util.List;

public record ApiError(Instant timestamp, int status, String code, String message, List<String> details) {

    public static ApiError of(int status, String code, String message, List<String> details) {
        return new ApiError(Instant.now(), status, code, message, details);
    }
}
