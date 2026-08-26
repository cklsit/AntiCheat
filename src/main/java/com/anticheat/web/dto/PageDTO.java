package com.anticheat.web.dto;

import java.util.List;

/**
 * 分页响应。与前端 Page&lt;T&gt; 对齐。
 */
public class PageDTO<T> {

    public List<T> list;
    public long total;
    public int page;
    public int pageSize;
    public int pages;

    public PageDTO() {
    }

    public PageDTO(List<T> list, long total, int page, int pageSize) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.pages = pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 0;
    }
}
