package com.tcm.user.enums;

import lombok.Getter;

/**
 * 账号状态。
 */
@Getter
public enum UserStatusEnum {

    DISABLED(0, "禁用"),
    ENABLED(1, "启用");

    private final int code;
    private final String desc;

    UserStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
