package com.tcm.common.base;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageResultTest {

    @Test
    void of_shouldFillFields() {
        List<String> records = Arrays.asList("a", "b");
        PageResult<String> page = PageResult.of(records, 100, 1, 10);
        assertEquals(2, page.getRecords().size());
        assertEquals(100, page.getTotal());
        assertEquals(1, page.getCurrent());
        assertEquals(10, page.getSize());
    }

    @Test
    void ofWithNullRecords_shouldNotThrow() {
        PageResult<String> page = PageResult.of(null, 0, 1, 10);
        assertNotNull(page.getRecords());
        assertTrue(page.getRecords().isEmpty());
    }
}
