package com.enterprise.platform.common.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/**
 * 分页查询基类。
 */
@Data
public class PageQuery implements Serializable {

    @Min(value = 1, message = "pageNum 最小为 1")
    private long pageNum = 1;

    @Min(value = 1, message = "pageSize 最小为 1")
    @Max(value = 500, message = "pageSize 最大为 500")
    private long pageSize = 10;
}
