package com.tcm.common.exception;

import lombok.Getter;

/**
 * 业务异常：业务层唯一允许抛出的异常，必须携带错误码。
 * 禁止在业务层抛出裸 RuntimeException。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
