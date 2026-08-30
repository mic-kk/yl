package com.tcm.common.utils;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

/**
 * 雪花 ID 生成器：全局唯一主键，应用层生成（workerId=1, datacenterId=1）。
 * 达梦/MySQL 双兼容；后期分库分表无主键冲突。线程安全。
 */
public final class SnowflakeIdUtil {

    private static final Snowflake SNOWFLAKE = IdUtil.getSnowflake(1, 1);

    private SnowflakeIdUtil() {
    }

    public static long nextId() {
        return SNOWFLAKE.nextId();
    }
}
