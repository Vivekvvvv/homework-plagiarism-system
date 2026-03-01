package com.example.homework.common;

import java.time.OffsetDateTime;

public record ApiResponse<T>(int code, String msg, T data, OffsetDateTime timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "OK", data, OffsetDateTime.now());
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(0, "OK", null, OffsetDateTime.now());
    }

    public static ApiResponse<Void> fail(int code, String msg) {
        return new ApiResponse<>(code, msg, null, OffsetDateTime.now());
    }
}
