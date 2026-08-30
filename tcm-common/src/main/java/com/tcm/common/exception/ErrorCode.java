package com.tcm.common.exception;

import lombok.Getter;

/**
 * 全局错误码枚举（分段约定）：
 * 200 成功；1xxx 通用；2xxx 鉴权；3xxx 参数校验；4xxx 业务（各业务域在后续迭代各自扩充分段）。
 */
@Getter
public enum ErrorCode {

    SUCCESS(200, "success"),

    /** 1xxx 通用 */
    SYSTEM_ERROR(1000, "系统繁忙，请稍后重试"),

    /** 2xxx 鉴权 */
    NOT_LOGIN(2001, "未登录或登录已过期"),
    NO_PERMISSION(2002, "无权限访问"),
    LOGIN_ERROR(2003, "用户名或密码错误"),

    /** 3xxx 参数校验 */
    PARAM_ERROR(3001, "参数校验失败"),

    /** 4xxx 业务 */
    USER_DISABLED(4001, "账号已被禁用");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
