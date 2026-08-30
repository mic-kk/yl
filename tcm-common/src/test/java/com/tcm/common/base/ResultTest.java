package com.tcm.common.base;

import com.tcm.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultTest {

    @Test
    void ok_shouldReturnSuccess() {
        Result<Void> result = Result.ok();
        assertTrue(result.isSuccess());
        assertEquals(200, result.getCode());
        assertNull(result.getData());
        assertTrue(result.getTimestamp() > 0);
    }

    @Test
    void okWithData_shouldCarryData() {
        Result<String> result = Result.ok("hello");
        assertTrue(result.isSuccess());
        assertEquals("hello", result.getData());
    }

    @Test
    void failWithErrorCode_shouldCarryCodeAndMessage() {
        Result<Void> result = Result.fail(ErrorCode.NOT_LOGIN);
        assertFalse(result.isSuccess());
        assertEquals(2001, result.getCode());
        assertEquals(ErrorCode.NOT_LOGIN.getMessage(), result.getMsg());
    }

    @Test
    void failWithCustomMessage_shouldUseCustomMessage() {
        Result<Void> result = Result.fail(ErrorCode.LOGIN_ERROR, "账号已被锁定");
        assertEquals(2003, result.getCode());
        assertEquals("账号已被锁定", result.getMsg());
    }
}
