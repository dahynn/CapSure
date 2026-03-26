package com.capsule.insurance.common.response;

import com.capsule.insurance.common.exception.ErrorCode;
import java.time.Instant;

public record ApiResponse<T>(
        boolean success,
        T data,
        String errorCode,
        String message,
        Instant timestamp
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, "OK", Instant.now());
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, data, null, message, Instant.now());
    }

    public static ApiResponse<Void> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, null, errorCode.name(), message, Instant.now());
    }
}
