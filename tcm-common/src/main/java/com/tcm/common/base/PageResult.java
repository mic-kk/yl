package com.tcm.common.base;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Getter;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 统一分页返回结构。
 */
@Getter
public class PageResult<T> implements Serializable {

    private final List<T> records;
    private final long total;
    private final long current;
    private final long size;

    private PageResult(List<T> records, long total, long current, long size) {
        this.records = records;
        this.total = total;
        this.current = current;
        this.size = size;
    }

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public static <T> PageResult<T> of(List<T> records, long total, long current, long size) {
        return new PageResult<>(records == null ? Collections.emptyList() : records, total, current, size);
    }
}
