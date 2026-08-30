package com.tcm.common.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * 敏感信息脱敏工具：审计日志、日志打印前必须脱敏。
 * 键名命中 SENSITIVE_KEYS 的值直接替换为掩码；嵌套 JSON 递归处理。
 */
public final class SensitiveDataUtil {

    private static final Set<String> SENSITIVE_KEYS = new HashSet<>() {{
        add("password");
        add("oldPassword");
        add("newPassword");
        add("idCard");
        add("phone");
        add("mobile");
        add("token");
        add("bankCard");
        add("certNo");
    }};

    private static final String MASK = "******";

    private SensitiveDataUtil() {
    }

    /** 对 JSON 字符串递归脱敏；非 JSON 输入原样返回。 */
    public static String maskJson(String json) {
        if (StrUtil.isBlank(json)) {
            return json;
        }
        try {
            Object parsed = JSONUtil.parse(json);
            return maskValue("", parsed);
        } catch (Exception e) {
            return json;
        }
    }

    /** 递归脱敏：命中敏感键名返回掩码；对象/数组递归处理；普通值原样返回。 */
    public static String maskValue(String key, Object value) {
        if (SENSITIVE_KEYS.contains(key)) {
            return MASK;
        }
        if (value instanceof JSONObject obj) {
            JSONObject masked = new JSONObject();
            for (String k : obj.keySet()) {
                masked.set(k, maskValue(k, obj.get(k)));
            }
            return masked.toString();
        }
        if (value instanceof JSONArray arr) {
            JSONArray masked = new JSONArray();
            arr.forEach(item -> masked.add(maskValue("", item)));
            return masked.toString();
        }
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }
}
