package com.zc.base.common.core;

import lombok.Data;

/**
 * 分页查询参数
 */
@Data
public class PageQuery {

    /** 页码，从1开始 */
    private int pageNum = 1;

    /** 每页大小 */
    private int pageSize = 10;

    /** 排序字段 */
    private String orderBy;

    /** 排序方向：asc / desc */
    private String orderDirection = "desc";

}
