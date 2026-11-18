package com.enterprise.platform.common.api;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 分页返回结构（不依赖具体 ORM 分页实现，便于跨模块复用）。
 */
@Data
public class PageResult<T> implements Serializable {

    private long total;
    private long pageNum;
    private long pageSize;
    private List<T> records = new ArrayList<>();

    public static <T> PageResult<T> of(long total, long pageNum, long pageSize, List<T> records) {
        PageResult<T> result = new PageResult<>();
        result.total = total;
        result.pageNum = pageNum;
        result.pageSize = pageSize;
        result.records = records == null ? new ArrayList<>() : records;
        return result;
    }

    public static <T> PageResult<T> empty(long pageNum, long pageSize) {
        return of(0, pageNum, pageSize, new ArrayList<>());
    }
}
