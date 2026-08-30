package com.tcm.common.base;

import com.tcm.common.constant.CommonConstant;
import com.tcm.common.exception.ErrorCode;
import lombok.Getter;

import java.io.Serializable;

/**
 * 全局统一返回结构。所有 Controller 方法必须返回 Result，禁止直接返回裸对象。
 */
@Getter
public class Result<T> implements Serializable {

    private final int code;
    private final String msg;
    private final T data;
    private final long timestamp;

    private Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> Result<T> ok() {
        return new Result<>(CommonConstant.SUCCESS_CODE, "success", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(CommonConstant.SUCCESS_CODE, "success", data);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public boolean isSuccess() {
        return this.code == CommonConstant.SUCCESS_CODE;
    }
}
