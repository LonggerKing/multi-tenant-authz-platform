package com.enterprise.platform.common.api;

import com.enterprise.platform.common.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结构。code == 0 表示成功，其余为业务错误码。
 */
@Data
public class ApiResult<T> implements Serializable {

    private int code;
    private String message;
    private T data;
    private long timestamp;

    public ApiResult() {
        this.timestamp = System.currentTimeMillis();
    }

    public ApiResult(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(0, "OK", data);
    }

    public static <T> ApiResult<T> ok() {
        return ok(null);
    }

    public static <T> ApiResult<T> fail(int code, String message) {
        return new ApiResult<>(code, message, null);
    }

    public static <T> ApiResult<T> fail(ErrorCode errorCode) {
        return new ApiResult<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public boolean isSuccess() {
        return code == 0;
    }
}
