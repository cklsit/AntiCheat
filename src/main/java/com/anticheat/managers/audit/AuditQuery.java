package com.anticheat.managers.audit;

/**
 * 审计日志查询条件。null 字段表示忽略该过滤项。
 */
public class AuditQuery {

    private String type;          // 类型过滤（精确匹配）
    private String result;        // 结果过滤（精确匹配）
    private String keyword;       // operator/target/detail 模糊匹配
    private Long startTime;       // 起始时间（含），epoch millis
    private Long endTime;         // 截止时间（含），epoch millis
    private int page = 1;
    private int pageSize = 20;

    public AuditQuery() {
    }

    public String getType() { return type; }
    public AuditQuery setType(String type) { this.type = type; return this; }

    public String getResult() { return result; }
    public AuditQuery setResult(String result) { this.result = result; return this; }

    public String getKeyword() { return keyword; }
    public AuditQuery setKeyword(String keyword) { this.keyword = keyword; return this; }

    public Long getStartTime() { return startTime; }
    public AuditQuery setStartTime(Long startTime) { this.startTime = startTime; return this; }

    public Long getEndTime() { return endTime; }
    public AuditQuery setEndTime(Long endTime) { this.endTime = endTime; return this; }

    public int getPage() { return Math.max(1, page); }
    public AuditQuery setPage(int page) { this.page = page; return this; }

    public int getPageSize() { return Math.max(1, Math.min(200, pageSize)); }
    public AuditQuery setPageSize(int pageSize) { this.pageSize = pageSize; return this; }
}
