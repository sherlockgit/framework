package com.sherlock.framework.base.web;

import com.sherlock.framework.base.error.ErrorCode;

public record ApiResponse<T>(boolean success, String code, String message, T data, String traceId) {

    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>(true, "OK", "OK", data, traceId);
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode, String traceId) {
        return new ApiResponse<>(false, errorCode.value(), errorCode.defaultMessage(), null, traceId);
    }
}
