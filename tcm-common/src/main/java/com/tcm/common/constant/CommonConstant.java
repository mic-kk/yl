package com.tcm.common.constant;

/** 全平台通用常量。业务常量放各自业务域 constant 包。 */
public final class CommonConstant {

    private CommonConstant() {
    }

    /** 统一返回：成功码 */
    public static final int SUCCESS_CODE = 200;
    /** 逻辑删除：未删除 */
    public static final int NOT_DELETED = 0;
    /** 逻辑删除：已删除 */
    public static final int DELETED = 1;
    /** 状态：启用 */
    public static final int STATUS_ENABLED = 1;
    /** 状态：禁用 */
    public static final int STATUS_DISABLED = 0;
    /** 分页默认页码 */
    public static final long DEFAULT_PAGE_NUM = 1L;
    /** 分页默认页大小 */
    public static final long DEFAULT_PAGE_SIZE = 10L;
}
