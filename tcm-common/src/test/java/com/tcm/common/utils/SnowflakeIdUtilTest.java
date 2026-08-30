package com.tcm.common.utils;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnowflakeIdUtilTest {

    @Test
    void nextId_shouldBeUniqueAndPositive() {
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 100_000; i++) {
            long id = SnowflakeIdUtil.nextId();
            assertTrue(id > 0, "雪花 ID 必须为正数");
            ids.add(id);
        }
        assertEquals(100_000, ids.size(), "10 万个 ID 必须无重复");
    }
}
