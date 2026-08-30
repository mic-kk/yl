package com.tcm.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensitiveDataUtilTest {

    @Test
    void maskJson_shouldMaskSensitiveKeysRecursively() {
        String json = "{\"username\":\"admin\",\"password\":\"123456\",\"profile\":{\"phone\":\"13800138000\"},\"list\":[{\"token\":\"abc\"}]}";
        String masked = SensitiveDataUtil.maskJson(json);
        assertFalse(masked.contains("123456"), "password 必须被脱敏");
        assertFalse(masked.contains("13800138000"), "嵌套 phone 必须被脱敏");
        assertFalse(masked.contains("\"abc\""), "嵌套 token 必须被脱敏");
        assertTrue(masked.contains("\"admin\""), "非敏感字段保留");
        assertTrue(masked.contains("******"));
    }

    @Test
    void maskJson_shouldReturnInputWhenNotJson() {
        assertEquals("plain text", SensitiveDataUtil.maskJson("plain text"));
        assertNull(SensitiveDataUtil.maskJson(null));
    }
}
