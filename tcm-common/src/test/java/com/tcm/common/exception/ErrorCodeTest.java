package com.tcm.common.exception;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ErrorCodeTest {

    @Test
    void errorCodes_shouldBeUniqueAndMessageNotEmpty() {
        long distinctCodes = Arrays.stream(ErrorCode.values()).map(ErrorCode::getCode).distinct().count();
        assertEquals(ErrorCode.values().length, distinctCodes, "错误码必须全局唯一");
        for (ErrorCode errorCode : ErrorCode.values()) {
            assertFalse(errorCode.getMessage() == null || errorCode.getMessage().isBlank(), "错误码信息不能为空");
        }
    }
}
