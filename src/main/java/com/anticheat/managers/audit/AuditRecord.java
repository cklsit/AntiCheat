package com.anticheat.managers.audit;

/**
 * 审计日志记录 POJO。
 * 字段与 web/dto/AuditDTO 字段语义对齐：
 * id/time/operator/operatorRole/type/target/ip/result/detail。
 */
public class AuditRecord {

    private Long id;             // 自增主键（SQL 场景）；非 SQL 场景可空
    private long timestamp;      // epoch millis
    private String operator;     // 操作者用户名
    private int operatorRole;    // 数字角色 0/1/2/3
    private String type;        // login / logout / ban / unban / config_change / case_verdict / ...
    private String target;      // 操作目标（玩家名、模块名、caseId 等）
    private String ip;          // 客户端 IP
    private String result;      // success / failed / warning
    private String detail;      // 详情

    public AuditRecord() {
    }

    public AuditRecord(long timestamp, String operator, int operatorRole, String type,
                       String target, String ip, String result, String detail) {
        this.timestamp = timestamp;
        this.operator = operator;
        this.operatorRole = operatorRole;
        this.type = type;
        this.target = target;
        this.ip = ip;
        this.result = result;
        this.detail = detail;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public int getOperatorRole() { return operatorRole; }
    public void setOperatorRole(int operatorRole) { this.operatorRole = operatorRole; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
